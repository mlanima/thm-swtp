# Discord Bot Refactoring Plan

> Based on the full codebase audit of `discord-bot/` (~1,175 source lines across 16 modules).
> Scope: SOLID, OOP, DRY improvements only — zero functionality changes.

---

## Current Architecture

```
                     +-------------------+
                     |  Java Backend     |
                     +--------+----------+
                              | (HTTP calls)
                              v
                 +---------------------------+
                 |  internalApi.ts (Express) |
                 +---------------------------+
                              ^
                     +--------+----------+
                     | Redis Streams     |
                     | - stream:discord:sync  | <-- Platform writes here
                     | - stream:platform:sync | --> Platform reads here
                     +--------+----------+
                              |
                 +------------+------------+
                 |                         |
          consumer.ts                producer.ts
                 |                         ^
                 v                         |
          discordQueue.ts            handlers/*.ts
                 |                         |
                 v                         |
          eventHandler.ts            Discord.js Client
                 |                         |
                 v                         |
            Discord API <------------------+
```

---

## Findings Summary

| Category | Issue | Files | Severity |
|----------|-------|-------|----------|
| SRP | God file (Express + auth + guild ops + channel ops + auto-setup + job ops) | `internalApi.ts` (278 lines) | High |
| SRP | Mixed concerns (channel resolution, embed building, truncation, send/edit/delete/invite/event) | `eventHandler.ts` (157 lines) | Medium |
| DIP | All modules import concrete singletons — no DI anywhere | Every module | Medium |
| DRY | Event type → job name mapping duplicated | `consumer.ts:27-33`, `discordQueue.ts:26-38` | Medium |
| DRY | Truncation logic duplicated inline | `eventHandler.ts:37,38,73` | Low |
| DRY | Channel resolution duplicated | `eventHandler.ts:19-25`, `internalApi.ts:22-37` | Low |
| Type Safety | `as any` on Redis/BullMQ connection | `discordQueue.ts:13,42` | Medium |
| Type Safety | `as unknown as Record<string, unknown>` payload casts | `producer.ts:26,30,34,38,42,46` | Medium |
| Type Safety | `err as unknown as { code: unknown }` error code extraction | `internalApi.ts:112,138,208` | Medium |
| Type Safety | DMChannel cast to `SendableChannel` | `eventHandler.ts:22` | Low |
| Error Handling | Magic-string error code matching (fragile) | `eventHandler.ts:92,132`, `consumer.ts:96` | Medium |
| Error Handling | Missing `.catch()` on async startup calls | `index.ts:37,38` | Low |
| Error Handling | No startup validation for `PLATFORM_API_SECRET` | `index.ts`, `internalApi.ts:14` | Low |
| Testing | ~5% coverage — only 1 test file (validator) | Whole project | High |

---

## Phase 1 — Low Risk (Mechanical, behaviour-preserving)

### 1.1 Extract shared truncation utility

**Problem:** Truncation logic for Discord embed limits (title 256 chars, description 4096 chars) is duplicated inline in `eventHandler.ts` at lines 37, 38, and 73.

**Solution:** Create `src/bot/utils/truncate.ts` with named constants and a shared function.

**Constants:**
- `EMBED_TITLE_MAX = 256`
- `EMBED_DESCRIPTION_MAX = 4096`

**Function:**
```ts
export function truncate(text: string, max: number): string {
  if (text.length <= max) return text;
  return text.slice(0, max - 3) + '...';
}
```

**Change:** Replace the 3 inline expressions with `truncate(title, EMBED_TITLE_MAX)` and `truncate(content, EMBED_DESCRIPTION_MAX)`.

**Files touched:** `eventHandler.ts` (3 lines), new file `src/bot/utils/truncate.ts`.

**Testability gain:** The truncation utility can be unit-tested in isolation.

### 1.2 Add missing `.catch()` handlers in `index.ts`

**Problem:** `startDiscordWorker()` (line 37) and `startInternalApi(PORT)` (line 38) have no `.catch()`. If they throw synchronously, it becomes an unhandled promise rejection.

**Solution:** Wrap both calls in a try-catch or add `.catch()`:
```ts
startDiscordWorker(); // sync-ish, but add try/catch if needed
startInternalApi(PORT); // could fail if port in use
```

**Change:** Wrap `startDiscordWorker()` and `startInternalApi(PORT)` in a try-catch with `logger.fatal`.

**Files touched:** `index.ts` (2 lines changed).

### 1.3 Add startup environment validation

**Problem:** Only `DISCORD_TOKEN` is validated (in `client.ts:29-31`). If `PLATFORM_API_SECRET` is unset, the internal API accepts all requests (the `validateSecret` middleware at line 15 lets requests through when both `secret` and `expected` are undefined/falsy).

**Solution:** Add a startup validation function in `index.ts` that checks all required env vars before starting subsystems.

**Variables to validate:**
- `DISCORD_TOKEN`
- `PLATFORM_API_SECRET` (if internal API is used)

**Files touched:** `index.ts` (add `validateEnv()` call).

---

## Phase 2 — Medium Risk (Extraction, no behavioural change)

### 2.1 Centralize event type mapping registry

**Problem:** The mapping from stream event type → job name is duplicated in `consumer.ts:27-33` and `discordQueue.ts:26-38`. Adding a new event requires touching 5 files (types, validator, consumer, queue, eventHandler).

**Solution:** Create `src/events/eventRegistry.ts` as a single source of truth:

```ts
import type { Job } from 'bullmq';
import type { ZodSchema } from 'zod';

export interface EventRegistration {
  jobName: string;
  schema: ZodSchema;
  defaultAttempts: number;
}

export const eventRegistry = {
  POST_CREATED: {
    jobName: 'sendPost',
    schema: PostCreatedSchema,
    defaultAttempts: 5,
  },
  POST_UPDATED: {
    jobName: 'editPost',
    schema: PostUpdatedSchema,
    defaultAttempts: 3,
  },
  POST_DELETED: {
    jobName: 'deletePost',
    schema: PostDeletedSchema,
    defaultAttempts: 3,
  },
  PROJECT_INVITE: {
    jobName: 'sendInvite',
    schema: ProjectInviteSchema,
    defaultAttempts: 3,
  },
  PROJECT_EVENT: {
    jobName: 'sendEvent',
    schema: ProjectEventSchema,
    defaultAttempts: 3,
  },
} as const;

export type StreamEventType = keyof typeof eventRegistry;
```

**Changes:**
- `consumer.ts:27-33` → `consumer.ts` derives `eventToJob` from the registry (removes hand-written map).
- `consumer.ts:35-41` → derives `jobAttempts` from the registry (removes hand-written map).
- `discordQueue.ts:26-38` → the switch statement moves into the registry or uses a lookup: `eventRegistry[job.name]?.handler`.
- `validator.ts:42-48` → `StreamPayloadSchema` is built from the registry (no hand-written discriminated union).

**Files touched:** `consumer.ts`, `discordQueue.ts`, `validator.ts`, new file `src/events/eventRegistry.ts`.

### 2.2 Extract channel resolution service

**Problem:** Channel resolution logic is duplicated in `eventHandler.ts:19-25` (`resolveSendableChannel`) and `internalApi.ts:22-37` (`resolveGuildChannel`). Both fetch a channel and check if it is text-based/sendable.

**Solution:** Create `src/bot/services/channelService.ts`:

```ts
import { discordClient } from '../client.js';
import type { TextChannel, NewsChannel, ThreadChannel, GuildBasedChannel, NonThreadGuildBasedChannel } from 'discord.js';

type SendableChannel = TextChannel | NewsChannel | ThreadChannel;

export function resolveSendableChannel(channelId: string): SendableChannel | null {
  const channel = discordClient.channels.resolve(channelId);
  if (!channel) return null;
  if (channel.isTextBased() && 'send' in channel) return channel as SendableChannel;
  return null;
}

export async function resolveGuildChannel(channelId: string) {
  const channel = await discordClient.channels.fetch(channelId);
  if (!channel || !channel.isTextBased() || !('guild' in channel) || !channel.guild) {
    return { channel: null, error: { status: 200, json: { success: false, reason: '...' } } };
  }
  return { channel: channel as GuildBasedChannel & NonThreadGuildBasedChannel, error: null };
}
```

**Changes:**
- `eventHandler.ts` replaces its inline `resolveSendableChannel` with the import.
- `internalApi.ts` replaces its inline `resolveGuildChannel` with the import.

**Files touched:** `eventHandler.ts`, `internalApi.ts`, new file `src/bot/services/channelService.ts`.

### 2.3 Extract embed builder utility

**Problem:** `eventHandler.ts` constructs embeds in 3 places (lines 35-41, 109-113, 148-151) with duplicated embed color constant and pattern.

**Solution:** Create `src/bot/utils/embedBuilder.ts`:

```ts
import { EmbedBuilder } from 'discord.js';
import { truncate } from './truncate.js';
import { EMBED_TITLE_MAX, EMBED_DESCRIPTION_MAX } from './truncate.js';

const NEUTRAL_COLOR = 0x99AAB5;

export function buildPostEmbed(title: string, content: string, authorName: string, platformUrl: string, authorAvatar?: string) {
  return new EmbedBuilder()
    .setAuthor({ name: authorName, iconURL: authorAvatar })
    .setTitle(truncate(title, EMBED_TITLE_MAX))
    .setDescription(truncate(content, EMBED_DESCRIPTION_MAX))
    .setColor(NEUTRAL_COLOR)
    .setURL(platformUrl)
    .setTimestamp();
}

export function buildEventEmbed(projectName: string, message: string) {
  return new EmbedBuilder()
    .setDescription(`${projectName} — ${message}`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();
}

export function buildInviteEmbed(projectName: string, inviterName: string) {
  return new EmbedBuilder()
    .setTitle(`Project Invitation: ${projectName}`)
    .setDescription(`You have been invited by **${inviterName}** to join the project **${projectName}**.`)
    .setColor(NEUTRAL_COLOR)
    .setTimestamp();
}
```

**Files touched:** `eventHandler.ts` (3 embed constructions replaced), `src/bot/utils/embedBuilder.ts` (new).

---

## Phase 3 — Architectural (Cleaner separation, zero behavioural change)

### 3.1 Split `internalApi.ts` into route modules

**Problem:** `internalApi.ts` (278 lines, 24% of all source code) is the biggest maintainability liability. It mixes Express setup, auth middleware, Discord permission logic, channel resolution, guild management, auto-setup orchestration, job retry, and server lifecycle.

**Solution:** Split into a layered structure:

```
src/http/
  server.ts              # Express app setup + lifecycle (start/stop)
  middleware/
    auth.ts              # validateSecret middleware
  routes/
    health.ts            # GET /health
    metrics.ts           # GET /metrics
    guilds.ts            # GET /internal/guilds
    channels.ts          # POST /internal/channels/:channelId/leave-guild
                         # POST /internal/channels/:channelId/restrict
                         # POST /internal/channels/:channelId/invite
                         # POST /internal/test-connection
    setup.ts             # POST /internal/auto-setup
    jobs.ts              # POST /internal/jobs/:jobId/retry
```

Each route module exports a `register` function:
```ts
export function register(router: express.Router): void;
```

`server.ts` imports and registers each module:
```ts
const app = express();
app.use(express.json());
registerHealthRoutes(app);
registerMetricsRoutes(app);
registerGuildRoutes(app);
// etc.
```

**Files touched:** `internalApi.ts` (removed), new files in `src/http/` directory.

### 3.2 Introduce dependency injection for testability

**Problem:** Every module imports concrete singletons (`redis`, `discordClient`, `logger`, `streamProducer`, etc.) directly. Unit testing requires `vi.mock()` for every import, which is fragile and makes tests hard to reason about.

**Solution:** Introduce a lightweight DI container or factory pattern for the key modules. Start with the core pipelines:

```ts
// src/streams/createConsumer.ts
export function createConsumer(deps: {
  redis: Redis;
  logger: Logger;
  queue: Queue;
  registry: typeof eventRegistry;
}) {
  // ...
}
```

```ts
// src/streams/createEventHandler.ts
export function createEventHandler(deps: {
  discordClient: Client;
  streamProducer: typeof streamProducer;
  logger: Logger;
  metrics: { messagesSent: Counter<string> };
}) {
  return {
    handleSendPost,
    handleEditPost,
    // ...
  };
}
```

```ts
// src/queues/createDiscordQueue.ts
export function createDiscordQueue(deps: {
  redis: Redis;
  logger: Logger;
  handlers: Record<string, (job: Job) => Promise<void>>;
}) {
  // ...
}
```

**This is the largest change.** Recommend deferring to Phase 3 and implementing selectively:
- `consumer.ts` → factory function first (high value for testing)
- `eventHandler.ts` → factory function (high value for testing)
- `discordQueue.ts` → factory function second

### 3.3 Fix unsafe casts

**Replace `as any` on BullMQ connection:**
BullMQ v5 accepts `ioredis` Redis instances directly (the types should match). Investigate the actual type mismatch. If BullMQ expects `ConnectionOptions` (which has `host`, `port`, etc.), wrap the redis instance:
```ts
// Instead of: connection: redis as any,
// Use:
connection: {
  host: redis.options.host,
  port: redis.options.port,
  retryStrategy: redis.options.retryStrategy,
},
```

**Or upgrade to BullMQ v5's typed connection:**
BullMQ v5 exports `RedisConnection` type. Use `new RedisConnection(redis)`.

**Replace `as unknown as Record<string, unknown>` in producer.ts:**
Define an explicit serialization mapping:
```ts
function serializePayload(payload: PlatformPayload): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(payload).map(([k, v]) => [k, v instanceof Array ? v : String(v)])
  );
}
```

**Replace `err as unknown as { code: unknown }` in internalApi.ts:**
Use Discord.js's `DiscordAPIError`:
```ts
import { DiscordAPIError } from 'discord.js';
if (err instanceof DiscordAPIError) {
  const code = err.code; // properly typed
}
```

### 3.4 Replace magic-string error matching with typed checks

**In `eventHandler.ts:92`** (`err.message.includes('10008')`):
Replace with:
```ts
import { DiscordAPIError } from 'discord.js';
if (err instanceof DiscordAPIError && err.code === 10008) {
  // Unknown Message
}
```

**In `eventHandler.ts:132`** (`err.message.includes('50007')`):
Replace with:
```ts
if (err instanceof DiscordAPIError && err.code === 50007) {
  // Cannot send messages to this user
}
```

**In `consumer.ts:96`** (`err.message.includes('NOGROUP')`):
Replace with a more robust check. Redis errors from ioredis have a `name` property:
```ts
if (err instanceof Error && err.message.includes('NOGROUP')) {
```
This is harder to type-check but at minimum document the dependency.

---

## Migration Plan

| Step | Phase | Effort | Files Changed | New Files | Testability Gain |
|------|-------|--------|---------------|-----------|------------------|
| 1.1 Shared truncation | 1 | 15 min | 1 | 1 | Medium |
| 1.2 Missing `.catch()` handlers | 1 | 5 min | 1 | 0 | None |
| 1.3 Startup env validation | 1 | 10 min | 1 | 0 | Low |
| 2.1 Centralized event registry | 2 | 1-2 h | 3 | 1 | High |
| 2.2 Channel resolution service | 2 | 30 min | 2 | 1 | Medium |
| 2.3 Embed builder utility | 2 | 30 min | 1 | 1 | Medium |
| 3.1 Split internalApi.ts | 3 | 2-3 h | 1 | 8+ | High |
| 3.2 DI for testability (selective) | 3 | 2-3 h | 3 | 3+ | Very High |
| 3.3 Fix unsafe casts | 3 | 30 min | 2 | 0 | Medium |
| 3.4 Magic-string → typed errors | 3 | 30 min | 2 | 0 | Low |

> **Phase 1:** ~30 min — Mechanical, safe, immediately beneficial.
> **Phase 2:** ~3 h — Structural improvements without behaviour change.
> **Phase 3:** ~5-7 h — Architectural cleanup, biggest testability gains.
> **Total estimate:** ~8-10 h for full implementation.

---

## Testing Strategy (after refactoring)

The current test coverage (~5%, only `validator.test.ts`) is the biggest quality gap. After refactoring:

| Module | Approach |
|--------|----------|
| `streams/validator.ts` | Already tested. |
| `src/bot/utils/truncate.ts` | Pure function — trivial unit tests. |
| `src/bot/services/channelService.ts` | Mock `discordClient.channels`. |
| `src/streams/consumer.ts` | Factory pattern — inject mock Redis + Queue. |
| `src/streams/producer.ts` | Factory pattern — inject mock Redis. |
| `src/streams/eventHandler.ts` | Factory pattern — inject mock Discord client + producer. |
| `src/queues/discordQueue.ts` | Factory pattern — inject mock Redis + handlers. |
| `src/http/routes/*.ts` | Inject mock Express router + Discord client. |
| `src/bot/handlers/*.ts` | Integration-level with mocked `streamProducer`. |
| `src/bot/client.ts` | Integration-level (skip — covered by E2E). |

**Priority order for new tests:**
1. `truncate.ts` — pure function, instant win
2. `eventHandler.ts` handlers — core business logic
3. `producer.ts` — all 6 produce methods
4. `consumer.ts` — stream processing loop
5. `internalApi.ts` routes — HTTP endpoints
6. `discordQueue.ts` — worker routing

---

## What NOT to change

- **Handler files** (`messageCreate.ts`, `messageUpdate.ts`, `messageDelete.ts`, `interactionCreate.ts`, `guildBanAdd.ts`, `channelDelete.ts`) — 17-45 lines each, well-factored, consistent pattern.
- **`wrapAsync.ts`** — clean utility.
- **`metrics/index.ts`** — fine as-is.
- **`config/redis.ts`**, **`config/logger.ts`** — infrastructure singletons, don't need abstraction.
- **Event-driven architecture** (Redis Streams + BullMQ) — sound design, keep.
- **Overall data flow** — inbound/outbound stream separation is correct; the changes are internal only.
