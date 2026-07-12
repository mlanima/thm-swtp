# Discord Integration Review

## Overview

The project implements a **two-service Discord integration**:

- **`discord-bot/`** — A standalone Node.js/TypeScript microservice using `discord.js` that connects to the Discord Gateway, listens to events, and exposes an internal HTTP API.
- **`api/.../discord/`** — A Spring Boot Java package within the backend that manages OAuth2, channel linking, notification settings, and bidirectional post sync.

Communication between the two services flows exclusively through **Redis Streams** (async/event-driven) and **HTTP** (synchronous control operations).

---

## Architecture

```
┌──────────────────────────────┐     HTTP (x-internal-secret)     ┌──────────────────────────────┐
│   Spring Boot Backend (api)  │ ◄─────────────────────────────►  │  discord-bot (discord.js)    │
│                              │     POST /internal/*             │   (TypeScript, Node.js)      │
│  ┌────────────────────────┐  │                                  │                              │
│  │ DiscordEventPublisher  │──┼── Redis Stream ────────────────►│  Stream Consumer             │
│  │  (stream:discord:sync) │  │   POST_CREATED, POST_UPDATED,   │  → BullMQ Queue              │
│  │                        │  │   POST_DELETED, PROJECT_INVITE, │  → Discord API (Gateway)     │
│  └────────────────────────┘  │   PROJECT_EVENT                 │                              │
│                              │                                  │  ┌────────────────────────┐  │
│  ┌────────────────────────┐  │  ◄── Redis Stream ──────────────┼──│ Stream Producer         │  │
│  │ PlatformSyncConsumer   │  │   DISCORD_MESSAGE_CREATED,      │  │  (stream:platform:sync) │  │
│  │  (polling @Scheduled)  │◄─┼── DISCORD_MESSAGE_UPDATED,      │  └────────────────────────┘  │
│  └────────────────────────┘  │   DISCORD_MESSAGE_DELETED,      │                              │
│                              │   DISCORD_MESSAGE_ASSIGNED,     │                              │
│                              │   INVITE_RESPONSE,              │                              │
│                              │   CHANNEL_DISCONNECTED          │                              │
└──────────────────────────────┘                                  └──────────────────────────────┘
```

### Redis Stream Structure

| Stream Name | Producer | Consumer | Purpose |
|---|---|---|---|
| `stream:discord:sync` | `api` (`DiscordEventPublisher`) | `discord-bot` consumer | Platform → Discord actions |
| `stream:platform:sync` | `discord-bot` producer | `api` (`PlatformSyncConsumer`) | Discord → Platform events |

### Communication Pattern

- **Async (Redis Streams)**: All content sync (posts, edits, deletes, invites, events) goes through Redis Streams with consumer groups for at-least-once delivery.
- **Sync (HTTP)**: Control operations (test-connection, create-invite, leave-guild, auto-setup, list-guilds, restrict-channel, retry-job) use HTTP with a shared `x-internal-secret` header.

---

## Key Findings

### 1. Custom JSON Serialization in Redis Publisher (Security & Integrity)

**File:** `api/.../discord/stream/DiscordEventPublisher.java:158-164`

```java
private String serialize(Map<String, ?> map) {
    return map.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":\"" + escape(...) + "\"")
            .reduce((a, b) -> a + "," + b)
            .map(s -> "{" + s + "}")
            .orElse("{}");
}
```

The publisher builds JSON manually instead of using Jackson. The consumer already has `ObjectMapper` available (`PlatformSyncConsumer.java:150`). The custom `escape()` method on line 166-172 handles basic escaping but could introduce subtle bugs (e.g. Unicode characters, control codes). The consumer on the bot side uses `JSON.parse` (`consumer.ts:66`), so there is potential for parsing failures if edge-case characters are not properly escaped.

**Recommendation:** Use Jackson's `ObjectMapper` to serialize the payload map instead of manual string building. Both the publisher and consumer have Jackson available.

---

### 2. Missing Stream Payload Validation in PlatformSyncConsumer

**File:** `api/.../discord/stream/PlatformSyncConsumer.java:139-163`

The consumer converts raw fields to `Map<String, String>` and directly parses the embedded JSON payload with `ObjectMapper`, but **never validates the structure** beyond a null check. If the bot publishes a malformed message, the consumer will fail at various points with cryptic errors. The discord-bot counterpart uses **Zod schemas** (`validator.ts`) for rigorous validation.

**Recommendation:** Add Zod-like validation on the Java side (e.g., Jakarta Bean Validation or a simple schema checker) for inbound stream messages to fail fast on malformed payloads.

---

### 3. Stream Acknowledgment on Processing Failure

**File:** `api/.../discord/stream/PlatformSyncConsumer.java:107-117`

```java
for (var record : records) {
    try {
        processRecord(record.getId(), ...);
        redis.opsForStream().acknowledge(...);
    } catch (Exception e) {
        log.error("Failed to process stream record {}: {}", record.getId(), e.getMessage());
    }
}
```

If `processRecord` throws an exception (e.g., constraint violation, parse error), the record is **not acknowledged**. This is correct for transient failures, but there is **no dead-letter handling** — failed records remain in the pending list forever and will be re-delivered on every poll, potentially causing an infinite retry loop.

The discord-bot side has a similar pattern (`consumer.ts:83-89`) where `ZodError` is acked and only unexpected errors are left unacked — though those also have no dead-letter.

**Recommendation:** Implement a pending-message reclamation strategy with a retry limit per message. After N failures, ack and log the error, or push to a dead-letter queue.

---

### 4. `deletePost` Job is Disabled in the Bot

**File:** `discord-bot/src/queues/discordQueue.ts:31-33`

```typescript
case 'deletePost':
    logger.info({ discordMsgId: job.data.discordMsgId }, 'deletePost disabled');
    return;
```

The `deletePost` job handler is intentionally bypassed — it logs the request and returns without calling `handleDeleteJob`. This means when a post is deleted on the platform, the Discord message **is never removed**. The `handleDeletePost` function exists in `eventHandler.ts` and works correctly, but the worker bypasses it.

**Recommendation:** Either re-enable the handler, or document the reason for its disablement clearly. If it's for safety, consider a config flag.

---

### 5. Synthetic `failedToday` in Status Response

**File:** `api/.../discord/service/DiscordStatusService.java:31-39`

```java
return new DiscordStatusResponse(
        link.isActive(),
        link.getDiscordChannelId(),
        syncedToday,
        0  // <-- hardcoded
);
```

`failedToday` is always **hardcoded to 0**. There is no mechanism to track or persist sync failures. The `DiscordMessageSyncEntity` has no failure-tracking column, and the `PlatformSyncConsumer` logs errors but never increments a failure counter.

**Recommendation:** Add a `failedAt` timestamp or a separate failure-log entity, and query it in `DiscordStatusService` for a real failure count.

---

### 6. Polling vs. Blocking Read in PlatformSyncConsumer

**File:** `api/.../discord/stream/PlatformSyncConsumer.java:93-121`

The consumer uses `@Scheduled(fixedDelay = 100)` with a blocking Redis read. This creates a **busy-polling** pattern: the scheduler fires every 100ms regardless of whether the previous poll completed or returned data. The `block(Duration.ofMillis(1500))` mitigates CPU waste, but the scheduling overhead is unnecessary when using blocking reads.

The discord-bot side uses **`setImmediate` with blocking `XREADGROUP`** (`consumer.ts:112-114`), which is more efficient — it only re-reads after the previous batch completes.

**Recommendation:** Switch to an event-driven loop (like the bot does) or use a thread-safe blocking pattern rather than a scheduled task.

---

### 7. Mixed REST API Styles in BotInternalClient

**File:** `api/.../discord/client/BotInternalClient.java`

The class **mixes two HTTP client APIs**:
- `RestTemplate` is configured as a bean in `DiscordStreamConfig.java` but is never used by this client.
- `RestClient` (Spring Boot 3.2+) is built anonymously per instance (`RestClient.builder().build()`), losing connection pooling and timeouts.

Every method has its own `try/catch` returning a failure response, which provides graceful degradation but makes the error handling verbose.

**Recommendation:** Either consolidate on `RestClient` as a single configured bean (with timeouts), or switch to `RestTemplate` (the existing bean). Avoid building a new HTTP client per request.

---

### 8. In-Memory OAuth2 State Without Persistence

**File:** `api/.../discord/service/DiscordAuthService.java`

All OAuth2 state tracking uses `ConcurrentHashMap`:
- `pendingStates` — user linking nonces
- `pendingBotNonces` — bot auth nonces
- `pendingBotGuilds` — guild ID pending storage

These are **ephemeral** — lost on restart, and cleared every 5 minutes by `purgeStaleBotNonces()`. The `purgeStaleBotNonces()` method **drops all nonces unconditionally** instead of individually expiring stale ones:

```java
@Scheduled(fixedRate = 300_000)
public void purgeStaleBotNonces() {
    pendingBotNonces.clear();  // clears ALL, not just stale
}
```

**Recommendation:** Use Redis or a database for OAuth2 state persistence, or at minimum expire individual entries by age rather than clearing the entire map.

---

### 9. Missing `DiscordChannelNotFoundException` Global Exception Handler

**File:** `api/.../discord/exception/DiscordChannelNotFoundException.java`

Three Discord-specific exceptions exist:
- `DiscordAccountAlreadyLinkedException` → handled → 409
- `DiscordConnectionFailedException` → handled → 400
- `DiscordChannelNotFoundException` → **NOT handled** in `GlobalExceptionHandler`

If thrown, this falls through to a generic 500 error.

**Recommendation:** Add a handler in `GlobalExceptionHandler` mapping this to 404.

---

### 10. Ghost User Profile Without Keycloak Registration

**File:** `api/.../discord/stream/PlatformSyncConsumer.java:189-196`

```java
UserProfile ghost = UserProfile.builder()
        .keycloakId(UUID.randomUUID())
        .username(discordUsername + "#discord")
        .discordId(discordUserId)
        .discordUsername(discordUsername)
        .build();
```

When a Discord message is synced to the platform from an unknown user, a **ghost profile** is created with a **random UUID as `keycloakId`**. This UUID is never registered in Keycloak, so the profile exists in the local DB but has no corresponding Keycloak user. Any system that relies on Keycloak authentication will not find this user.

**Recommendation:** Consider whether these ghost profiles need to be identifiable in Keycloak, or add a flag to distinguish platform users from Discord-only users.

---

### 11. Bot Internal API — No Global Error Handler

**File:** `discord-bot/src/http/internalApi.ts`

Each route has its own `try/catch` with inline error responses. There is no centralized Express error handler. This leads to:
- Inconsistent error response shapes (`{ success: false, reason: "..." }` vs `{ error: "..." }`)
- Potential unhandled promise rejections in some paths

The `/health` and `/metrics` endpoints are unprotected (no `validateSecret` middleware), which is correct for Prometheus scraping and load balancers.

**Recommendation:** Add a centralized Express error-handling middleware and ensure consistent response shapes.

---

### 12. Redis Connection Shared Between BullMQ and Streams

**File:** `discord-bot/src/config/redis.ts`, `discord-bot/src/queues/discordQueue.ts:13`

Both BullMQ and the Redis stream operations use the **same** Redis connection instance. BullMQ expects exclusive use of the connection for its internal mechanics (blocking operations, connection state). Sharing may lead to unpredictable behavior during blocking reads.

```typescript
export const discordQueue = new Queue('discord-actions', {
  connection: redis as any,  // shared with stream consumer/producer
});
```

**Recommendation:** Create a separate Redis connection instance for BullMQ.

---

### 13. `CHANNEL_DISCONNECTED` Events From Bot-Ban Handler Use Inefficient Looping

**File:** `discord-bot/src/bot/handlers/guildBanAdd.ts:9-14`

```typescript
const channels = ban.guild.channels.cache.filter((c) => c.isTextBased());
for (const [, channel] of channels) {
    await streamProducer.channelDisconnected({
        channelId: channel.id,
        reason: 'BOT_BANNED',
    });
}
```

When the bot is banned from a guild, it fires a `CHANNEL_DISCONNECTED` event for **every text channel**, one at a time (serial `await` in a loop). For large guilds, this creates N sequential Redis writes. The platform consumer then deactivates links one by one.

**Recommendation:** Parallelize the writes with `Promise.all()` or batch the disconnections into a single event with multiple channel IDs.

---

### 14. Redis Stream Message Format Inconsistency

The bot's **producer** (`producer.ts`) writes:

```
type: "DISCORD_MESSAGE_CREATED"
payload: JSON.stringify({...})
```

The bot's **consumer** (`consumer.ts`) reads:

```
type: fieldMap.type      (raw string from field list)
payload: JSON.parse(fieldMap.payload)
```

The Java **publisher** writes:

```
type: "POST_CREATED"
payload: serialize({...})   (custom JSON string)
```

The Java **consumer** reads with `ObjectMapper` from the field map. Both sides are **compatible** but use different serialization strategies. The key concern is that the **Java publisher uses hand-rolled JSON** which may not handle all edge cases that Jackson would.

---

### 15. `DiscordStreamConfig` — Unused RestTemplate Bean

**File:** `api/.../discord/config/DiscordStreamConfig.java:15-17`

```java
@Bean
public RestTemplate discordRestTemplate() {
    return new RestTemplate();
}
```

This bean is only used by `DiscordOAuthClient.java` (injected via constructor). The `BotInternalClient.java` does **not** use it — it builds its own `RestClient`. Consider removing the unused bean or consolidating all HTTP calls.

---

## Minor Observations

| Issue | Location | Note |
|---|---|---|
| Unused `discordRestTemplate` bean | `DiscordStreamConfig.java:15` | Only `DiscordOAuthClient` uses it |
| Typo in comment: "reconn" | `PlatformSyncConsumer.java` (truncated output) | Minor |
| `@SuppressWarnings` rawtypes | `DiscordOAuthClient.java` | Avoid raw `Map` returns |
| `DiscordMessageSyncEntity.direction` never set by `PlatformSyncConsumer` for `DISCORD_TO_PLATFORM` | `PlatformSyncConsumer.java:214` | Actually it is set correctly on line 214 |
| `LinkedChannelEntity` warning field in response | `DiscordChannelResponse.java` | Optional warning (e.g., `BOT_NO_WRITE_PERMISSION`) — good pattern |
| No rate-limit handling | Both sides | Discord API rate limits are not explicitly handled beyond BullMQ retries |
| Prometheus metrics on bot side are defined but `redisStreamLag` and `queueDepth` are never updated | `discord-bot/src/metrics/index.ts:19-28` | Gauges are never set to actual values |

---

## Data Model Summary

```
ProjectEntity (1) ──── (1) LinkedChannelEntity (1) ──── (1) DiscordChannelSettingsEntity
                                │
                                │ (many)
                                ▼
                    DiscordMessageSyncEntity
                    (platformPostId ↔ discordMessageId)
                    (direction: PLATFORM_TO_DISCORD | DISCORD_TO_PLATFORM)

UserProfile:
  - discordId
  - discordUsername
  - discordAvatar
  - discordConnectedAt
```

---

## Data Flow Walkthrough

### Platform Post → Discord (Outbound)
1. `ProjectPostService` calls `DiscordEventPublisher.publishPostCreated(post)`
2. Publisher finds active `LinkedChannelEntity` for the project
3. Writes `{ type: "POST_CREATED", payload: { ... } }` to `stream:discord:sync`
4. Bot's `StreamConsumer` reads, validates with Zod, enqueues to BullMQ as `sendPost`
5. BullMQ worker calls `handleSendPost`, sends embed to Discord channel
6. Worker calls `streamProducer.discordMessageAssigned()` with the assigned Discord message ID
7. Java `PlatformSyncConsumer` reads `DISCORD_MESSAGE_ASSIGNED`, saves sync mapping

### Discord Message → Platform Post (Inbound)
1. Bot's `messageCreate` handler produces `DISCORD_MESSAGE_CREATED` to `stream:platform:sync`
2. Java `PlatformSyncConsumer` reads it (polling every 100ms)
3. Creates `ProjectPostEntity` and `DiscordMessageSyncEntity` (direction: `DISCORD_TO_PLATFORM`)
4. If Discord user doesn't exist in DB, creates a "ghost" profile

### Channel Linking Flow
1. User authorizes bot via OAuth2 → guild owned by the user
2. User calls `POST /api/v1/projects/{id}/discord/auto-connect`
3. Backend calls `POST /internal/auto-setup` on bot → creates `#posts` channel with restricted perms
4. Backend calls `POST /internal/channels/{id}/invite` → creates permanent invite
5. Backend creates `LinkedChannelEntity` and `DiscordChannelSettingsEntity`

---

## Recommendations Summary (Priority Order)

1. **Re-enable or document `deletePost`** — Currently disabled without explanation
2. **Replace custom JSON serialization** in `DiscordEventPublisher` with Jackson
3. **Add stream payload validation** on the Java consumer side (like Zod on the bot side)
4. **Implement dead-letter handling** for failed stream messages on both sides
5. **Track failures** in `DiscordStatusService` instead of hardcoding `failedToday=0`
6. **Fix OAuth2 purge** to expire individually by age, not clear the whole map
7. **Add Redis connection for BullMQ** separate from stream operations
8. **Handle `DiscordChannelNotFoundException`** in `GlobalExceptionHandler`
9. **Switch PlatformSyncConsumer to event-driven loop** instead of `@Scheduled(fixedDelay)`
10. **Parallelize `CHANNEL_DISCONNECTED` events** in guildBanAdd handler
11. **Consolidate HTTP clients** — configure a single `RestTemplate` or `RestClient` bean
