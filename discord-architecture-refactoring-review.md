# Discord Integration — Architecture & Refactoring Review

## Overview

The discord module spans **two services** and **~2500 lines of code** across 27 Java + 12 TypeScript source files. It implements bidirectional post sync, channel linking, notifications, OAuth2 account linking, and bot management — all through Redis Streams and HTTP.

This review evaluates the code against SOLID, OOP, and DRY principles. **No functional changes are proposed** — only structural improvements that preserve all current behaviour.

---

## 1. Single Responsibility Principle

### 1.1 `PlatformSyncConsumer` — Consumer, dispatcher, and handler in one class

**File:** `api/.../discord/stream/PlatformSyncConsumer.java`

The class does three distinct jobs:

| Responsibility | Evidence | Lines |
|---|---|---|
| Stream infrastructure | `ensureGroup()`, `poll()`, `reclaimPending()` | 79-154 |
| Event dispatch | `processRecord()` switch on 6 event types | 156-181 |
| Business logic | `handleDiscordMessageCreated()`, `handleDiscordMessageUpdated()`, etc. | 183-319 |

**Problem:** Adding a new event type means touching the same class in three places (dispatch, handler method, validation). The class is 320 lines with `@Transactional` on every handler — mixing stream mechanics with business logic.

**Refactor:** Split into:
- `PlatformSyncConsumer` — stream polling, group management, acknowledgment (stays lean)
- `DiscordEventHandler` — processes individual `Map<String, String>` payloads, all `@Transactional` methods moved here
- `EventType` enum — central registry of known event types

The consumer delegates to `eventHandler.process(type, payload)` instead of a switch.

### 1.2 `DiscordChannelService.connectChannel()` — Procedural 77-line method

**File:** `api/.../discord/service/DiscordChannelService.java:47-123`

One method does all of:
1. Validate owner has Discord linked
2. Deactivate old project link
3. Deactivate cross-project guild links
4. Check channel not linked to another project
5. Test bot connection
6. Reuse or create `LinkedChannelEntity`
7. Create Discord invite
8. Save entity
9. Create default settings if missing

**Problem:** Violates SRP and makes the method impossible to unit-test in isolation. Any change to one sub-step risks breaking others.

**Refactor:** Extract small private methods (`deactivateOldLink`, `ensureNoCrossProjectLink`, `ensureChannelAvailable`, `createInvite`, `ensureDefaultSettings`) and compose them in `connectChannel()`.

### 1.3 `DiscordAuthController.callback()` — 4 flows in one handler

**File:** `api/.../discord/controller/DiscordAuthController.java:36-106`

The single `callback` endpoint branches on `state` prefix into 4 distinct OAuth flows:

| Flow | Prefix | Response |
|---|---|---|
| Bot token exchange | `bot:` + has `code` | `window.close()` |
| Bot guild-only | `bot:` + has `guildId` | `window.close()` |
| User linking | `user:` | Redirect to frontend |
| Legacy bot | bare UUID state | `window.close()` |

**Problem:** The 70-line method is hard to reason about. Error handling differs per flow (some use redirects, some use `window.close()`), but all are interleaved in one `if/else` chain. Adding a 5th flow means modifying the same method.

**Refactor:** Move each flow into a separate private method with a clear name, or use a `Map<StatePrefix, FlowHandler>` dispatch. The common `closePopupResponse()` is already extracted; do the same for each flow path.

### 1.4 `DiscordEventPublisher` — Publishing + notification filtering + URL building

**File:** `api/.../discord/stream/DiscordEventPublisher.java`

This component today:
- Publishes 6 event types to Redis
- Filters by notification settings (`shouldNotify`)
- Truncates content to 3900 chars
- Builds post URLs

**Problem:** URL building (`buildPostUrl`) is an infrastructural concern mixed with event publishing. Notification filtering (`shouldNotify`) is business logic.

**Refactor:** Extract `PostUrlBuilder` (or move to `ProjectService`). Notification filtering belongs near the notification settings domain, not in the publisher.

---

## 2. Open/Closed Principle

### 2.1 Event types are scattered switch statements

Adding a new event type requires modifying **at minimum** these locations:

| Side | File | What changes |
|---|---|---|
| API | `DiscordEventPublisher` | New `publish*()` method |
| API | `PlatformSyncConsumer.processRecord()` | New `case` in switch |
| Bot | `consumer.ts` | New entry in `eventToJob` map |
| Bot | `discordQueue.ts` | New `case` in worker switch |
| Bot | `eventHandler.ts` | New handler function |
| Bot | `producer.ts` | New method on `streamProducer` |
| Both | `events.ts` / (Java none) | New type definition |

**Problem:** Every addition modifies 5+ files across two services. The Java side has no central event type registry (the bot's `events.ts` is the only truth).

**Refactor (Java):** Introduce an `EventType` enum and a `StreamEvent` record:

```java
public enum EventType {
    POST_CREATED, POST_UPDATED, POST_DELETED,
    PROJECT_INVITE, PROJECT_EVENT
}

public record StreamEvent(EventType type, Map<String, String> payload) {}
```

Use a `Map<EventType, EventHandler>` in the consumer instead of a switch. New event types only need a new handler class + enum entry.

**Refactor (Bot):** The bot already has a typed enum (`StreamEventType`) and a dispatch map (`eventToJob`). The map is an improvement over a switch — but it's a plain object, not a `Map`. Consider `new Map<StreamEventType, string>()` for type safety.

### 2.2 Bot internal API routes are tightly coupled to Express

**File:** `discord-bot/src/http/internalApi.ts`

Every route is registered directly on the Express app with inline middleware and inline error handling. Adding a new endpoint means adding a new `internalApi.verb(...)` call with duplicated error boilerplate.

**Refactor:** Use Express `Router` and register route groups. Extract an error-handling wrapper:

```typescript
function asyncHandler(fn: (req, res, next) => Promise<void>) {
    return (req, res, next) => fn(req, res, next).catch(next);
}
```

Central error handler: `internalApi.use((err, req, res, next) => res.status(500).json({ success: false, reason: err.message }))`.

---

## 3. Dependency Inversion Principle

### 3.1 External services depend on `LinkedChannelRepository` directly

**Files:**
- `ProjectService.java:56` — `private final LinkedChannelRepository linkedChannelRepository;`
- `ProjectInviteMapper.java:13` — same
- `ProjectInviteController.java:24` — same

Three classes outside the discord package call `linkedChannelRepository.findByProjectId()`. This leaks the persistence layer into callers.

**Refactor:** Add a single method to `DiscordChannelService` (or a new `DiscordQueryService`):

```java
public Optional<String> getInviteUrl(UUID projectId) {
    return linkedChannelRepository.findByProjectId(projectId)
            .filter(LinkedChannelEntity::isActive)
            .map(LinkedChannelEntity::getDiscordInviteUrl);
}
```

All external callers depend on the service interface, not the repository. This also eliminates the duplicated `Optional` chaining in `ProjectInviteMapper` and `ProjectInviteController`.

### 3.2 `BotInternalClient` has no interface

The HTTP client that talks to the discord-bot is a concrete class. Every caller (`DiscordChannelService`) depends directly on it.

**Refactor:** Extract `BotOperations` interface with the 8 operations, keeping `BotInternalClient` as the HTTP implementation. This enables testing with stubs and reduces the blast radius if the bot protocol changes.

### 3.3 `DiscordOAuthClient` mixes two communication channels

The class talks to `discord.com` (OAuth2 endpoints) — a completely different external system than the internal bot. It's grouped in the same `client` package but has no shared abstraction with `BotInternalClient`.

**Refactor:** The `client` package currently means "HTTP clients." If both implement a `DiscordApiClient` interface, the package would have a clear contract. Alternatively, rename to `oauth/` and `bot/` sub-packages.

---

## 4. DRY Violations

### 4.1 Content truncation repeated 3 times

```java
// DiscordEventPublisher.java:39-41, 67-69, 131
if (content.length() > 3900) {
    content = content.substring(0, 3897) + "...";
}
```

The magic number `3900` (Discord embed description limit minus JSON escaping overhead) appears three times.

**Refactor:**

```java
private static final int DISCORD_CONTENT_MAX = 3900;

private static String truncate(String text, int max) {
    if (text == null || text.length() <= max) return text;
    return text.substring(0, max - 3) + "...";
}
```

### 4.2 `BotInternalClient` — 8 identical try/catch templates

Every method follows the same pattern:

```java
try {
    return restClient.post()...retrieve().body(ResponseType.class);
} catch (Exception e) {
    log.error("Bot <action> failed: {}", e.getMessage());
    return new ResponseType(false, ...);
}
```

**Refactor:** See 1.1 — extract a `callBot()` template method. Even a private helper that takes a `Supplier<T>` and a failure supplier reduces the 8 blocks to 1.

### 4.3 Owner Discord ID check duplicated

**File:** `DiscordChannelService.java:51-54` and `194-197`

```java
if (project.getOwner().getDiscordId() == null || project.getOwner().getDiscordId().isBlank()) {
    throw new DiscordConnectionFailedException("You must link your Discord account...");
}
```

Appears in both `connectChannel()` and `autoConnectChannel()`.

**Refactor:** Extract `validateOwnerDiscordLinked(ProjectEntity)`.

### 4.4 Duplicate sync-mapping creation in `PlatformSyncConsumer`

The sync entity is built twice with nearly identical code:

| Location | Direction |
|---|---|
| `handleDiscordMessageCreated:227-233` | `DISCORD_TO_PLATFORM` |
| `handleMessageAssigned:290-298` | `PLATFORM_TO_DISCORD` |

The builder is identical except for the `direction` field and `guildId`. Both could delegate to `DiscordPostSyncService.saveSync()` which already exists and does the same thing.

---

## 5. OOP Design Observations

### 5.1 Rich domain vs. anemic entities

The three JPA entities (`LinkedChannelEntity`, `DiscordMessageSyncEntity`, `DiscordChannelSettingsEntity`) are pure data holders with Lombok getters/setters. All logic lives in services.

**Refactor (minor):** Move simple queries to the entity or a helper:

```java
// In DiscordChannelSettingsEntity
public boolean shouldNotify(String eventType) {
    return switch (eventType) {
        case "MEMBER_JOIN" -> notifyMemberJoin;
        case "MEMBER_LEAVE" -> notifyMemberLeave;
        default -> true;
    };
}
```

Eliminates `DiscordEventPublisher.shouldNotify()` entirely.

### 5.2 `DiscordNotificationService` is a thin passthrough

```java
public void notifyMemberJoined(...) {
    eventPublisher.publishProjectEvent(projectId, "MEMBER_JOIN", memberName + " joined the project");
}
```

Every method is a one-liner delegating to `DiscordEventPublisher`. The service adds no value over calling the publisher directly.

**Refactor (optional):** Either inline into the callers (eliminating the service) or make it a proper facade that enriches events with notification settings checks. In its current form it's a middle layer without abstraction benefit.

### 5.3 Raw `Map<String, String>` as a universal payload type

The Java side passes Discord event payloads as `Map<String, String>` everywhere — in the publisher, the stream consumer, all handler methods. This means:
- No compile-time type safety for payload fields
- IDE can't autocomplete field names
- Refactoring a field name is a text search, not a rename operation

The bot side does this right: `events.ts` defines typed interfaces (`PostCreatedPayload`, `ProjectInvitePayload`, etc.) with specific fields.

**Refactor:** Create Java records for each payload:

```java
public record PostCreatedPayload(
    UUID postId, UUID projectId, String channelId,
    String content, String title, String authorName,
    String platformUrl, String authorAvatar
) {}

public record DiscordMessageCreatedPayload(
    String discordMsgId, String channelId, String content,
    String discordUserId, String discordUsername, List<String> attachmentUrls
) {}
```

The consumer's handlers become type-safe instead of `payload.get("discordMsgId")`.

---

## 6. Architecture & Coupling

### 6.1 Current package structure

```
discord/
  client/        — HTTP clients (two different services, no shared interface)
  config/        — Properties + bean definitions
  controller/    — 4 REST controllers
  dto/           — 3 response DTOs
  entity/        — 3 JPA entities
  exception/     — 3 exception classes
  repository/    — 3 JPA repositories
  service/       — 5 services
  stream/        — Redis stream publisher + consumer
```

### 6.2 Cross-package dependencies (outside discord/)

```
project/ProjectService
  → LinkedChannelRepository        (read: invite URL)
  → DiscordNotificationService     (write: member left)

projectPost/ProjectPostService
  → DiscordEventPublisher          (write: post created/updated/deleted)
  → DiscordPostSyncService         (read/write: sync mapping)

projectInvitation/ProjectInviteMapper
  → LinkedChannelRepository        (read: invite URL)

projectInvitation/ProjectInviteController
  → LinkedChannelRepository        (read: invite URL)

exceptionhandling/GlobalExceptionHandler
  → DiscordAccountAlreadyLinkedException
  → DiscordConnectionFailedException
```

### 6.3 Issue: Leaky repository access

`LinkedChannelRepository` is the most reused discord type outside the package (4 external references). This means:
- If the entity mapping changes, 4 files need updating
- If the query logic becomes more complex (e.g., caching, permissions), there's no single place to add it

**Refactor:** Consider a `DiscordProjectService` that exposes coordinated operations:

```java
@Service
public class DiscordProjectService {
    public Optional<String> getActiveInviteUrl(UUID projectId) { ... }
    public boolean isChannelLinked(UUID projectId) { ... }
    public void onMemberRemoved(UUID projectId, String memberName) { ... }
    public void onPostEvent(UUID postId, PostEventType type) { ... }
}
```

This single point of entry replaces `LinkedChannelRepository` + `DiscordNotificationService` + `DiscordEventPublisher` + `DiscordPostSyncService` scattered across 3 callers.

---

## 7. Cross-Cutting Patterns

### 7.1 Error handling asymmetry

| Layer | Error style | Issue |
|---|---|---|
| Java controllers | Proper HTTP status codes via exceptions | Good |
| Java `BotInternalClient` | All errors → `"bot unreachable"` | Masks 401 vs 500 vs timeout |
| Bot `internalApi.ts` | `{ success: false, reason }` or `{ error }` | Two shapes |
| Java `PlatformSyncConsumer` | Silent `debug` log on poll errors | Errors invisible |

### 7.2 No retry strategy for stream consumer

Failed records remain pending forever. The bot's BullMQ worker has exponential backoff and configurable retries. The Java consumer has neither — a transient DB failure means the record retries every 100ms until it succeeds.

**Refactor:** Add a retry counter to the pending-message reclamation. Ack after 3 failures and log the dropped event.

### 7.3 Stringly-typed event type constants

Event types are string literals duplicated across both services:

| Value | Appears in |
|---|---|
| `"POST_CREATED"` | `DiscordEventPublisher.java:59`, `consumer.ts:28`, `validator.ts:43` |
| `"DISCORD_MESSAGE_CREATED"` | `PlatformSyncConsumer.java:170`, `events.ts:62` |
| `"MEMBER_JOIN"` | `DiscordNotificationService.java:20`, `DiscordEventPublisher.java:153` |

Shared constants (or an enum) would prevent typos and enable IDE navigation.

---

## 8. Suggested Refactoring Roadmap

### Phase 1 — Low risk, high consistency (2-3h)

| Change | Files affected | Principle |
|---|---|---|
| Extract `truncate()` utility | `DiscordEventPublisher` | DRY |
| Remove unused `clientId` field | `DiscordChannelService` | — |
| Remove redundant `BotNonce` constructor | `DiscordAuthService` | — |
| Replace `purgeStaleBotNonces().clear()` with age-based expiry | `DiscordAuthService` | Correctness |
| Add `DISCORD_CONTENT_MAX` constant | `DiscordEventPublisher` | DRY |
| Move `shouldNotify()` into `DiscordChannelSettingsEntity` | `DiscordEventPublisher` | OOP |

### Phase 2 — Medium risk, structural (4-6h)

| Change | Files affected | Principle |
|---|---|---|
| Extract `BotOperations` interface | `BotInternalClient`, `DiscordChannelService` | DIP |
| Add template method for bot HTTP calls | `BotInternalClient` | DRY |
| Create EventType enum + StreamEvent record | `PlatformSyncConsumer`, `DiscordEventPublisher` | OCP |
| Split `PlatformSyncConsumer` into consumer + handler | `PlatformSyncConsumer` | SRP |
| Split `DiscordAuthController.callback()` into flow handlers | `DiscordAuthController` | SRP |

### Phase 3 — Higher risk, architectural (8-16h)

| Change | Files affected | Principle |
|---|---|---|
| Create typed payload records for all events | `PlatformSyncConsumer`, `DiscordEventPublisher` | OOP |
| Add `DiscordProjectService` facade | All 3 external callers + `LinkedChannelRepository` | DIP |
| Replace raw `Map<String, String>` request bodies with DTOs | `DiscordChannelController` | OOP |
| Add dead-letter mechanism to stream consumer | `PlatformSyncConsumer` | Robustness |
| Centralize Express error handling in bot | `internalApi.ts` | DRY |
| Create `PostUrlBuilder` component | `DiscordEventPublisher` | SRP |
