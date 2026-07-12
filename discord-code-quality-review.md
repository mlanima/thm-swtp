# Discord Integration — Code Quality Review

## 1. `BotInternalClient.java` — Boilerplate & Error Masking

**File:** `api/.../discord/client/BotInternalClient.java`

### 1.1 Eight identical try/catch blocks

Every method duplicates the same pattern: call bot → catch `Exception` → return failure DTO with `"bot unreachable"`.

```java
public CreateInviteResponse createChannelInvite(...) {
    try {
        return restClient.post()...retrieve().body(CreateInviteResponse.class);
    } catch (Exception e) {
        log.error("Bot create-invite failed ...");
        return new CreateInviteResponse(false, null, "bot unreachable");
    }
}
```

**Problem:** Adding a new endpoint means copy-pasting 8 lines of error handling. The `"bot unreachable"` string masks real errors (401, 500, connection refused) — the caller gets the same message regardless of whether the bot is down or rejecting the request.

**Refactor:** Extract a helper method:

```java
private <T> T callBot(String action, Supplier<T> call, T failureResponse) {
    try {
        return call.get();
    } catch (Exception e) {
        log.error("Bot {} failed: {}", action, e.getMessage());
        return failureResponse;
    }
}
```

Or use a `RestClient` status handler to differentiate HTTP errors from connectivity errors.

### 1.2 Unconfigured `RestClient`

```java
private final RestClient restClient = RestClient.builder().build();
```

**Problem:** No connect timeout, no read timeout, no connection pooling. A hanging bot instance blocks the API thread indefinitely.

### 1.3 Raw `success`/`reason` response records

Every response DTO has `boolean success` + `String reason` — they're structurally identical but each is a separate record type. This works but creates 8 near-identical types for what could be a generic `BotResponse<T>`.

---

## 2. `DiscordOAuthClient.java` — Inconsistent HTTP Clients

**File:** `api/.../discord/client/DiscordOAuthClient.java`

### 2.1 Mixed HTTP client APIs

The project has **three** HTTP client patterns:

| File | Client | Construction |
|---|---|---|
| `DiscordOAuthClient` | `RestTemplate` | Injected bean (`discordRestTemplate`) |
| `BotInternalClient` | `RestClient` | `RestClient.builder().build()` (raw, unconfigured) |
| `DiscordStreamConfig` | `RestTemplate` | `new RestTemplate()` (bean, unused by BotInternalClient) |

**Refactor:** Pick one client (preferably `RestClient` with a configured bean), delete the unused `discordRestTemplate` bean.

### 2.2 Duplicate form-body construction

`exchangeBotCode()` and `exchangeCode()` build identical OAuth2 form bodies. Only the response parsing differs.

### 2.3 `new ObjectMapper()` per request

`exchangeBotCode()` creates a fresh `ObjectMapper` for JSON parsing on every call. Should be a field (or reuse the injected bean if one existed).

---

## 3. `DiscordAuthService.java` — OAuth2 State Management

**File:** `api/.../discord/service/DiscordAuthService.java`

### 3.1 Destructive nonce purge

```java
@Scheduled(fixedRate = 300_000)
public void purgeStaleBotNonces() {
    pendingBotNonces.clear();  // nukes ALL nonces, even fresh ones
}
```

Clears every nonce every 5 minutes — users who start an OAuth flow near the 5-minute boundary lose their nonce. The `PendingBotGuild` cleanup (line 170-171) does this correctly with `removeIf(PendingBotGuild::isExpired)`.

### 3.2 Manual constructor despite Lombok convention

The rest of the project uses `@RequiredArgsConstructor`. This class has a 10-line explicit constructor mixing injected services and `@Value` fields. Could use `@RequiredArgsConstructor` with `@Value` on constructor parameters.

### 3.3 No cross-instance sharing

`ConcurrentHashMap` is volatile — lost on restart, invisible to other API instances. For horizontal scaling (which the project is designed for), OAuth2 state must live in Redis or the database.

### 3.4 Redundant record constructor

```java
private record BotNonce(UUID projectId, UUID userId) {
    BotNonce(UUID projectId, UUID userId) {
        this.projectId = projectId;
        this.userId = userId;
    }  // identical to the canonical constructor — remove
}
```

Records auto-generate this constructor. The explicit one is dead code.

---

## 4. `PlatformSyncConsumer.java` — Resilience & Error Handling

**File:** `api/.../discord/stream/PlatformSyncConsumer.java`

### 4.1 `ensureGroup()` message checking is fragile

`e.getMessage()` may return `"Error in execution"` (a wrapper from `StringRedisTemplate`) instead of the underlying Redis error. The `contains("BUSYGROUP")` and `contains("NOGROUP")` checks will fail on wrapped exceptions, causing the consumer to never recover.

**Fix:** Walk the `getCause()` chain to find the actual Redis error message.

### 4.2 No dead-letter mechanism

Records that fail processing remain in the pending list forever and are re-read on every poll cycle (infinite retry). Should ack after N failures and log the dropped event.

### 4.3 Unchecked cast

```java
(Map<String, Object>) (Map<?, ?>) record.getValue()
```

### 4.4 Unnecessary HashMap allocation in hot path

`processRecord()` creates a `new HashMap<>()` on every invocation (every stream record). For the common case where `rawFields` already contains `type` and `payload`, the copy is wasted.

---

## 5. `DiscordEventPublisher.java` — Hardcoded URL & Duplicated Truncation

**File:** `api/.../discord/stream/DiscordEventPublisher.java`

### 5.1 Production URL hardcoded

```java
private String buildPostUrl(ProjectPostEntity post) {
    return "https://swtp-ss26.de/project/" + post.getProject().getProjectUrl();
}
```

This URL is baked into code, not configurable. Different environments (dev, staging) will produce incorrect links in Discord embeds.

### 5.2 Truncation logic duplicated 3×

```java
if (content.length() > 3900) content = content.substring(0, 3897) + "...";
```

Appears in `publishPostCreated`, `publishPostUpdated`, and `publishProjectEvent`. Could be a shared `truncate(String, int)` utility.

### 5.3 `ObjectMapper` created per instance

```java
private final ObjectMapper objectMapper = new ObjectMapper();
```

Minor, but if multiple `DiscordEventPublisher` instances existed (they don't with `@Component`, but still a pattern concern).

---

## 6. `DiscordAuthController.java` — Monolithic Callback

**File:** `api/.../discord/controller/DiscordAuthController.java`

### 6.1 `callback()` handles 4 distinct flows in one method

The 70-line `callback` method branches on `state` prefix for:
1. Bot token exchange (`bot:` + code)
2. Bot guild-only (`bot:` + guildId, no code)
3. User linking (`user:`)
4. Legacy bot (bare UUID state)

Each flow has different error semantics (popup close vs redirect), but they're interleaved with `if/else` chains. Hard to reason about, hard to test.

**Refactor:** Split into separate handler methods (e.g., `handleBotCallback`, `handleUserCallback`, `handleLegacyCallback`) or use an enum dispatch.

### 6.2 `handleBotTokenExchange` silences errors

```java
private ResponseEntity<?> handleBotTokenExchange(String nonce, String code) {
    try {
        discordAuthService.handleBotCallback(nonce, code);
    } catch (Exception e) {
        log.warn("Bot token exchange callback failed: {}", e.getMessage());
    }
    return closePopupResponse();  // same response for success AND failure
}
```

The user gets no feedback if the token exchange fails (e.g., guild owner mismatch). The Discord popup just closes silently.

---

## 7. `DiscordChannelController.java` — Typeless Request Bodies

**File:** `api/.../discord/controller/DiscordChannelController.java`

### 7.1 Request bodies are raw `Map<String, String>`

```java
public ResponseEntity<DiscordChannelResponse> connect(
        @PathVariable UUID projectId,
        @RequestBody Map<String, String> body) {     // <-- untyped
```

Every endpoint accepts a bare `Map` instead of a typed DTO. No schema validation (keys are extracted with `.get()` and null-checked manually). Swagger/OpenAPI docs can't describe the expected shape.

**Refactor:** Create small request DTOs:
```java
public record ConnectRequest(@NotBlank String channelId, String guildId) {}
public record UpdateInviteRequest(@NotBlank String discordInviteUrl) {}
```

### 7.2 Missing `guildId` validation

`connect` validates `channelId` for null/blank but accepts any `guildId` (including null, empty, or missing from body). If `guildId` is meant to be optional, the null case should be handled explicitly, not silently passed through.

---

## 8. `DiscordChannelService.java` — Feature Envy & Performance

**File:** `api/.../discord/service/DiscordChannelService.java`

### 8.1 `connectChannel()` does everything

Validates owner, deactivates old links, deactivates cross-project guild links, checks cross-project channel conflicts, tests bot connection, reuses or creates link entity, creates invite, saves, creates default settings — **all in one `@Transactional` method**. 77 lines, hard to unit-test, hard to override single steps.

### 8.2 Unused `clientId` field

```java
@Value("${DISCORD_CLIENT_ID:}")
private String clientId;  // never read
```

### 8.3 `getAvailableGuilds` loads the entire table

```java
linkedChannelRepository.findAll().stream()
    .filter(LinkedChannelEntity::isActive)
    .map(LinkedChannelEntity::getDiscordGuildId)
```

`findAll()` loads every `linked_channels` row into memory just to extract active guild IDs. At scale (thousands of projects), this is a performance problem. Should use a derived query or a native query.

---

## 9. Bot Internal API (`internalApi.ts`) — No Validation & Inconsistent Shapes

**File:** `discord-bot/src/http/internalApi.ts`

### 9.1 No request body validation

Unlike the stream consumer which uses **Zod** schemas for validation (`validator.ts`), the HTTP endpoints accept raw `req.body` with zero validation. Missing fields produce `undefined` that flows into Discord API calls.

### 9.2 Inconsistent error response shapes

| Endpoint | Error shape |
|---|---|
| `validateSecret` middleware | `{ error: "unauthorized" }` |
| All other endpoints | `{ success: false, reason: "..." }` |

Callers (Java `BotInternalClient`) deserialize each endpoint's response into a specific record, so each endpoint effectively has its own contract. But the middleware's 401 response has a different shape than all other errors.

### 9.3 No centralized error handler

Every route has its own `try/catch`. Express's default error handler ( `(err, req, res, next) => {}` ) is never used.

### 9.4 `restrict` endpoint uses raw REST API

```typescript
const rest = discordClient.rest;
await rest.put(`/channels/${channelId}/permissions/${roleId}`, { body: ... });
```

Mixed with discord.js patterns. discord.js has `channel.permissionOverwrites.edit()` / `create()` which are safer and cached. The raw REST bypasses the cache.

---

## Summary: Top 5 Refactoring Candidates

| Priority | Area | Why |
|---|---|---|
| 1 | `PlatformSyncConsumer.ensureGroup()` | Fragile exception message checking breaks consumer group recovery |
| 2 | `BotInternalClient` | 8× duplicated try/catch + unconfigured HTTP client + error masking |
| 3 | `DiscordAuthService` nonce purge | Destructive `clear()` destroys valid nonces every 5 min |
| 4 | Controller request bodies as raw `Map` | No validation, no OpenAPI docs, null checks everywhere |
| 5 | `buildPostUrl` hardcoded prod URL | Wrong links in every non-production environment |
