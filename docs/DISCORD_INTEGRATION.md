# Discord Integration

## Overview

IdeaCamp integrates with Discord to provide project-connected Discord servers. Each
project can link a dedicated Discord channel for automated post syncs, with a
restricted `#posts` channel (owner-only posting). Users can link their Discord
identity via OAuth2 for a profile badge and ownership verification.

---

```
┌──────────────────────────────────────────┐
│           Angular Frontend               │
│  (Profile Badge, Project Settings,       │
│   Discord Tab)                           │
└───────────────────┬──────────────────────┘
                    │ HTTP (REST)
                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                      Spring Boot API                             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Discord Module                                           │   │
│  │  ┌──────────────┐  ┌───────────────┐  ┌───────────────┐  │   │
│  │  │ Controllers  │  │ Services      │  │ Clients       │  │   │
│  │  │ (Auth,       │──┤ (Channel,     │──│ DiscordOAuth  │  │   │
│  │  │  Channel,    │  │  Auth,        │  │ BotInternal   │  │   │
│  │  │  Settings,   │  │  PostSync,    │  │ DiscordProps  │  │   │
│  │  │  Status)     │  │  Status)      │  │               │  │   │
│  │  └──────────────┘  └───────────────┘  └───────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────┬────────────────────────────────────┬────────────────┘
             │                                    │
             │  ┌─ HTTP (shared secret) ──┐       │  ┌─ spring-data-redis ──────┐
             │  │  /internal/*            │       │  │  (Redis Streams)          │
             │  │                         │       │  │                           │
             │  │  Auto-Setup             │       │  │  DiscordEventPublisher   │
             │  │  Channel Connect        │       │  │  → stream:discord:sync   │
             │  │  Create Invite          │       │  │                           │
             │  │  Leave Guild            │       │  │  PlatformSyncConsumer    │
             │  │  Restrict Channel       │       │  │  ← stream:platform:sync  │
             │  │  List Guilds            │       │  └───────────────────────────┘
             │  │  Test Connection        │       │
             │  │  Job Retry              │       │
             │  │  Health                 │       │
             │  └─────────────────────────┘       │
             ▼                                    ▼
┌───────────────────────────────┐    ┌──────────────────────────────┐
│        Discord Bot            │    │         Redis               │
│        (Node.js)              │◄───│  (Redis Streams)             │
│                               │    │                              │
│  ┌─────────────────────────┐  │    │  stream:discord:sync        │
│  │ http/server.ts          │  │    │    (API → Bot)              │
│  │ http/routes/*           │──┤────│                              │
│  │ http/middleware/auth.ts │  │    │  stream:platform:sync       │
│  ├─────────────────────────┤  │    │    (Bot → API)              │
│  │ streams/consumer.ts     │◄─┤────│                              │
│  │ streams/eventHandler.ts │  │    │                              │
│  │ queues/discordQueue.ts  │  │    │  BullMQ queue:              │
│  ├─────────────────────────┤  │    │  discord-actions            │
│  │ streams/producer.ts     │──┤───►│  (5 job types)              │
│  ├─────────────────────────┤  │    └──────────────────────────────┘
│  │ bot/client.ts           │  │
│  │ bot/handlers/*          │  │
│  │ bot/services/*          │  │
│  │ bot/utils/*             │  │
│  │ bot/wrapAsync.ts        │  │
│  ├─────────────────────────┤  │
│  │ events/eventRegistry.ts │  │
│  │ types/events.ts         │  │
│  │ config/redis.ts         │  │
│  │ config/logger.ts        │  │
│  │ metrics/index.ts        │  │
│  └─────────────────────────┘  │
└──────────────┬────────────────┘
               │
               │ Discord Gateway (WebSocket) + REST API
               │ (discord.js)
               │
               ▼
┌───────────────────────────────┐
│        Discord.com            │
│   (Gateway + REST API)        │
│                               │
│   api.discord.com             │
│   gateway.discord.com         │
└───────────────────────────────┘

────────── OAuth2 Flow (Browser-vermittelt) ──────────

┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  1. Browser → Discord.com                                           │
│     Nutzer wird weitergeleitet zur Discord-OAuth2-Seite mit         │
│     state=bot:<nonce> oder state=user:<nonce> (Nonce serverseitig)  │
│                                                                     │
│  2. Discord.com → Browser                                           │
│     Nutzer autorisiert → Discord zeigt Consent-UI → Redirect zurück │
│     an /api/v1/auth/discord/callback mit Code und State             │
│                                                                     │
│  3. Spring API → Discord.com (direkt, server-seitig)                │
│     Tauscht Code + client_secret gegen Access-Token via RestTemplate│
│     → Antwort enthält access_token, guild-Objekt, owner_id          │
│                                                                     │
│  4. Spring API validiert Nonce, prüft owner_id, speichert Guild     │
│     und schließt das Popup (window.close())                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Components

| Component            | Stack                           | Location                                 |
| -------------------- | ------------------------------- | ---------------------------------------- |
| **Spring Boot API**  | Java 25, Spring Boot 4          | `api/` package `de.thm.swtp.api.discord` |
| **Discord Bot**      | Node.js, discord.js v14, BullMQ | `discord-bot/`                           |
| **Angular Frontend** | Angular 21, PrimeIcons          | `web/ideacamp/`                          |
| **Redis**            | Redis Streams + BullMQ          | `redis.ser.mlanima.org:6379`             |

---

## Flows

### User Discord Link

1. User clicks "Link Discord" in User Settings → `GET /api/v1/auth/discord/authorize`
2. API generates a random nonce, stores it in `pendingStates` as `user:<nonce>`
3. User is redirected to Discord's OAuth2 consent page (`scope=identify`)
4. Discord redirects back to `/callback?code=...&state=user:<nonce>`
5. Server validates the nonce, exchanges the code for an access token
6. Fetches `/users/@me`, stores Discord ID/username/avatar on `UserProfile`

### Project → Discord Connection

1. User must have linked their Discord account (profile check)
2. Frontend calls `GET /projects/{projectId}/discord/bot-invite` (authenticated, `@PreAuthorize`)
3. API generates a bot nonce (`bot:<nonce>` in `pendingBotNonces`) and returns a Discord
   authorization URL with `scope=bot%20identify&response_type=code`
4. User is redirected to Discord, selects a server, authorizes the bot
5. Discord redirects to `/callback?code=...&state=bot:<nonce>`

**Two sub-flows for the callback:**

**a) With `code` (full OAuth, "Requires OAuth2 Code Grant" enabled):**

- `bot:` prefix branch validates the nonce via `consumeBotNonce`
- `DiscordOAuthClient.exchangeBotCode()` exchanges the code with Discord's token endpoint
- Token response includes `guild` object (`id`, `name`, `owner_id`)
- Ownership check: `guild.owner_id` must match the project owner's Discord ID
- Guild is stored in `pendingBotGuilds[projectId]`
- Popup closes → `autoConnect()` picks up the pending guild

**b) Without `code` but with `guild_id` (pure bot add, no identify scope):**

- Same `bot:` prefix branch, nonce is still validated
- `handleBotGuildOnly(nonce, guildId)` stores the guild in `pendingBotGuilds`
- No guild ownership check (Discord doesn't return guild info without the code exchange)
- Popup closes → auto-connect picks it up

6. If auto-connect fails due to "bot in multiple guilds", the frontend shows a
   guild picker (fetched via `GET /projects/{projectId}/discord/guilds`)
7. `POST /projects/{projectId}/discord/auto-connect` with `guildId` calls the bot's
   `/internal/auto-setup` → bot finds or creates `#posts` channel

**Existing channel behavior:**

- If `#posts` already exists, the bot checks `SendMessages` permission for itself
- If lacking `SendMessages`, returns `canWrite: false`
- Frontend shows an amber warning (`PROJECTSETTINGS.DISCORD.BOT_NO_WRITE_PERMISSION`)
- Auto-setup still succeeds and channel is linked

### Post Sync

1. User creates/updates/deletes a post on the platform
2. API publishes an event to Redis stream `stream:discord:sync`
3. Bot's stream consumer picks up the event, validates it (Zod), and enqueues a BullMQ job
4. BullMQ worker executes the corresponding handler (`sendPost` / `editPost` / `deletePost`)
5. Bot sends, edits, or deletes an embed message in the linked channel via Discord API

> **Note:** The platform does not currently implement a post-edit feature for content changes.
> `POST_UPDATED` is only emitted when a post is **archived** (status change).
> The `editPost` handler is implemented and tested but unused until content editing is added.

#### Deletion direction (one-way)

- **App → Discord**: ✅ when a post is deleted on the platform, the bot
  removes the corresponding embed from the Discord channel
- **Discord → App**: ❌ deleting a bot message in Discord does **not**
  delete the post in the app. This prevents a sync loop: app deletes post
  → bot deletes embed → Discord fires `MessageDelete` → bot would
  re-trigger the deletion. Bot-authored messages are skipped intentionally.

### Disconnect

1. User clicks "Disconnect" in Project Settings
2. API deactivates `LinkedChannelEntity` (sets `isActive = false`)
3. API calls `POST /internal/channels/:channelId/leave-guild` on the bot
4. Bot leaves the Discord guild

---

## Backend — REST API

### Discord Auth (`/api/v1/auth/discord`)

| Method   | Endpoint      | Auth    | Description                                                 |
| -------- | ------------- | ------- | ----------------------------------------------------------- |
| `GET`    | `/authorize`  | ✅      | Generate user link URL (returns `{ url }`)                  |
| `GET`    | `/callback`   | ❌ (\*) | OAuth2 callback — handles `bot:`, `user:`, and legacy flows |
| `DELETE` | `/disconnect` | ✅      | Unlink Discord account from user profile                    |

(\*) `permitAll()` by design (Discord redirects browsers here). Nonce validation
prevents CSRF/poisoning attacks — see Security section.

### Discord Channel (`/api/v1/projects/{projectId}/discord`)

| Method   | Endpoint        | Auth | Description                                    |
| -------- | --------------- | ---- | ---------------------------------------------- |
| `GET`    | `/bot-invite`   | ✅   | Get bot invite URL (`{ url }`)                 |
| `POST`   | `/auto-connect` | ✅   | Auto-create/find `#posts` via bot internal API |
| `GET`    | `/connect`      | ✅   | Get current connection status                  |
| `POST`   | `/connect`      | ✅   | Manually link channel by ID + optional guildId |
| `PATCH`  | `/invite`       | ✅   | Update stored Discord invite URL               |
| `DELETE` | `/connect`      | ✅   | Disconnect (deactivate + bot leaves guild)     |
| `GET`    | `/guilds`       | ✅   | List available guilds (for guild picker)       |

### Discord Settings (`/api/v1/projects/{projectId}/discord/settings`)

| Method | Endpoint | Auth | Description                                                         |
| ------ | -------- | ---- | ------------------------------------------------------------------- |
| `GET`  | `/`      | ✅   | Get notification settings (post created/deleted, member join/leave) |
| `PUT`  | `/`      | ✅   | Update notification settings                                        |

### Discord Status (`/api/v1/projects/{projectId}/discord/status`)

| Method | Endpoint | Auth | Description                                                 |
| ------ | -------- | ---- | ----------------------------------------------------------- |
| `GET`  | `/`      | ✅   | Status: `{ isActive, channelId, syncedToday, failedToday }` |

- `syncedToday` counts messages synced for this project's channel since midnight
- `failedToday` is reserved (currently always 0, no failure tracking infrastructure yet)

### Bot Internal API (not publicly exposed — port 3001)

| Method | Endpoint                                    | Description                                        |
| ------ | ------------------------------------------- | -------------------------------------------------- |
| `GET`  | `/health`                                   | Redis + Discord client health                      |
| `GET`  | `/metrics`                                  | Prometheus metrics                                 |
| `GET`  | `/internal/guilds`                          | List guilds the bot is in                          |
| `POST` | `/internal/auto-setup`                      | Find or create `#posts` with permission overwrites |
| `POST` | `/internal/channels/:channelId/invite`      | Create invite link                                 |
| `POST` | `/internal/channels/:channelId/leave-guild` | Bot leaves guild                                   |
| `POST` | `/internal/channels/:channelId/restrict`    | Restrict channel to owner + bot only               |
| `POST` | `/internal/test-connection`                 | Verify bot can access a channel                    |
| `POST` | `/internal/jobs/:jobId/retry`               | Retry a failed BullMQ job                          |

Authenticated via `x-internal-secret` header matching `PLATFORM_API_SECRET`.
**Fail-closed**: if the env var is unset, all requests are rejected.

---

## Backend — Key Classes (`api/`)

| Class                          | File                  | Purpose                                                                                     |
| ------------------------------ | --------------------- | ------------------------------------------------------------------------------------------- |
| `DiscordAuthController`        | `discord/controller/` | OAuth2 redirect + callback (handles `bot:`, `user:`, legacy flows)                          |
| `DiscordChannelController`     | `discord/controller/` | Channel connect/disconnect/auto-setup/guilds                                                |
| `DiscordSettingsController`    | `discord/controller/` | Notification settings CRUD                                                                  |
| `DiscordStatusController`      | `discord/controller/` | Connection status endpoint                                                                  |
| `DiscordAuthService`           | `discord/service/`    | OAuth2 state management, nonce generation/validation, token exchange, guild ownership check |
| `DiscordChannelService`        | `discord/service/`    | Channel linking, one-server-per-project enforcement                                         |
| `DiscordStatusService`         | `discord/service/`    | Status queries with per-project sync counts                                                 |
| `DiscordOAuthClient`           | `discord/client/`     | HTTP client for Discord OAuth2 token + user info                                            |
| `BotInternalClient`            | `discord/client/`     | HTTP client for bot internal API (shared secret auth)                                       |
| `DiscordProperties`            | `discord/config/`     | `@ConfigurationProperties` for `discord.*`                                                  |
| `DiscordStreamConfig`          | `discord/config/`     | Redis Stream config for async bot communication                                             |
| `LinkedChannelEntity`          | `discord/entity/`     | JPA: project ↔ Discord channel mapping                                                      |
| `DiscordChannelSettingsEntity` | `discord/entity/`     | JPA: per-channel notification toggles                                                       |
| `DiscordMessageSyncEntity`     | `discord/entity/`     | JPA: synced message tracking (dedup, sync counts)                                           |

### Nonce System (`DiscordAuthService`)

Three concurrent maps manage CSRF-safe OAuth flows:

| Map                | Key → Value                                              | TTL    | Purpose                                  |
| ------------------ | -------------------------------------------------------- | ------ | ---------------------------------------- |
| `pendingStates`    | nonce (String) → userId (UUID)                           | 5 min  | User Discord link flow (`user:<nonce>`)  |
| `pendingBotNonces` | nonce (String) → `BotNonce{projectId, userId}`           | 5 min  | Bot authorization flow (`bot:<nonce>`)   |
| `pendingBotGuilds` | projectId (UUID) → `PendingBotGuild{guildId, createdAt}` | 30 min | Bridge between callback and auto-connect |

All maps are purged periodically via `@Scheduled` methods.

### Callback State Resolution

```
state = "bot:abc-123"     → bot: prefix → consume nonce abc-123
                                     ├── code present    → full OAuth exchange + guild ownership check
                                     └── no code, guildId → store guild via validated nonce

state = "user:xyz-789"    → user: prefix → consume nonce → link Discord account

state = bare UUID         → legacy branch → require isPendingBotAuth() check
                           (dead code for current flow, kept as safety net)
```

---

## Frontend — Components (`web/ideacamp/`)

| Component                      | Location                                     | Purpose                               |
| ------------------------------ | -------------------------------------------- | ------------------------------------- |
| `user-settings/discord-tab`    | `feature/user-settings/tabs/discord-tab/`    | Link/unlink Discord account           |
| `project-settings/discord-tab` | `feature/project-settings/tabs/discord-tab/` | Connect/disconnect server, invite URL |
| `profile-banner`               | `shared/profile-banner/`                     | Discord badge with copy-to-clipboard  |

### Profile Badge

1. **Initial**: Small pill with Discord icon
2. **Click 1**: Reveals username, switches to outlined style
3. **Click 2**: Copies `username#tag` to clipboard → shows "Copied!" for 2s → resets

### Auto-Connect on Tab Return

After the Discord OAuth redirect, when the browser tab regains focus, the
project-settings component checks `localStorage` for `discord-pending-setup`
and triggers `autoConnect()` if the project matches.

### Signal Bindings

`ngModel` bindings use `[ngModel]="signal()" (ngModelChange)="signal.set($event)"`
rather than `[(ngModel)]="signal"` to avoid reassigning the `WritableSignal` function.

---

## Discord Bot (`discord-bot/`)

| File                          | Purpose                                                    |
| ----------------------------- | ---------------------------------------------------------- |
| `index.ts`                    | Entry point, validates env, starts all subsystems          |
| `bot/client.ts`               | Discord.js client setup with intents                       |
| `bot/wrapAsync.ts`            | Error wrapper for async event handlers (fire-and-forget)   |
| `bot/handlers/messageCreate.ts` | Forwards user messages from Discord → platform stream    |
| `bot/handlers/messageUpdate.ts` | Forwards message edits from Discord → platform stream    |
| `bot/handlers/messageDelete.ts` | Forwards message deletions from Discord → platform stream |
| `bot/handlers/interactionCreate.ts` | Handles invite Accept / Decline button interactions    |
| `bot/handlers/channelDelete.ts` | Notifies platform when a linked channel is removed        |
| `bot/handlers/guildBanAdd.ts` | Disconnects all channels when the bot itself is banned     |
| `bot/services/channelService.ts` | Channel resolution helpers (resolve by ID, fetch guild) |
| `bot/utils/truncate.ts`       | Text truncation with ellipsis for Discord embed limits     |
| `bot/utils/embedBuilder.ts`   | Embed builders for posts, events, and invite DMs           |
| `http/server.ts`              | Express app assembly, start/stop lifecycle                  |
| `http/internalApi.ts`         | Thin re-export: `startInternalApi` / `stopInternalApi`     |
| `http/middleware/auth.ts`     | `x-internal-secret` validation middleware                   |
| `http/routes/health.ts`       | `GET /health` — Redis + Discord status                     |
| `http/routes/metrics.ts`      | `GET /metrics` — Prometheus endpoint                       |
| `http/routes/guilds.ts`       | `GET /internal/guilds` — list joined guilds                |
| `http/routes/channels.ts`     | Channel: leave, restrict, invite, test-connection          |
| `http/routes/setup.ts`        | `POST /internal/auto-setup` — find or create `#posts`      |
| `http/routes/jobs.ts`         | `POST /internal/jobs/:id/retry` — retry failed BullMQ job  |
| `streams/consumer.ts`         | Redis stream consumer: reads `stream:discord:sync`         |
| `streams/producer.ts`         | Redis stream producer: writes to `stream:platform:sync`    |
| `streams/eventHandler.ts`     | Factory-pattern handlers for BullMQ job types              |
| `streams/validator.ts`        | Zod schemas for stream event payload validation            |
| `queues/discordQueue.ts`      | BullMQ queue + worker for Discord actions                  |
| `events/eventRegistry.ts`     | Single source of truth: event type → job name / schema     |
| `metrics/index.ts`            | Prometheus metrics registry (counters, gauges)             |
| `config/logger.ts`            | Pino logger with dev pretty-print                          |
| `config/redis.ts`             | ioredis connection + typed BullMQ connection config        |
| `types/events.ts`             | TypeScript interfaces for all stream payloads              |

### BullMQ Jobs

| Job Name      | Handler             | Purpose                                     |
| ------------- | ------------------- | ------------------------------------------- |
| `sendPost`    | `handleSendPost`    | Send embed message to Discord channel       |
| `editPost`    | `handleEditPost`    | Edit embed description (currently unused — platform has no content editing yet) |
| `deletePost`  | `handleDeletePost`  | Delete embed from Discord channel           |
| `sendInvite`  | `handleSendInvite`  | Send invite DM with Accept / Decline button |
| `sendEvent`   | `handleSendEvent`   | Send project event notification embed       |

---

## Redis Streams

| Stream                 | Direction | Payload                                                                  |
| ---------------------- | --------- | ------------------------------------------------------------------------ |
| `stream:discord:sync`  | API → Bot | `POST_CREATED`, `POST_UPDATED` (archive only), `POST_DELETED`, `PROJECT_INVITE`, `PROJECT_EVENT` |
| `stream:platform:sync` | Bot → API | `DISCORD_MESSAGE_ASSIGNED`, `DISCORD_MESSAGE_CREATED`, `DISCORD_MESSAGE_UPDATED`, `DISCORD_MESSAGE_DELETED`, `INVITE_RESPONSE`, `CHANNEL_DISCONNECTED` |

API publishes to `stream:discord:sync` via `DiscordEventPublisher` (Spring Data Redis).
Bot consumes with `streams/consumer.ts` (XREADGROUP loop), validates with Zod schemas,
and enqueues BullMQ jobs. Bot writes back to `stream:platform:sync` via `streams/producer.ts`.

---

## Configuration

### API Environment Variables

| Variable                 | Required | Default                 | Description                                            |
| ------------------------ | -------- | ----------------------- | ------------------------------------------------------ |
| `DISCORD_BOT_BASEURL`    | No       | `http://localhost:3001` | Bot internal API URL                                   |
| `DISCORD_BOT_API_SECRET` | No       | (empty)                 | Shared secret (must match bot's `PLATFORM_API_SECRET`) |
| `DISCORD_CLIENT_ID`      | No       | (empty)                 | Discord OAuth2 client ID                               |
| `DISCORD_CLIENT_SECRET`  | No       | (empty)                 | Discord OAuth2 client secret                           |
| `DISCORD_REDIRECT_URI`   | No       | (empty)                 | OAuth2 callback URL                                    |
| `SPRING_DATA_REDIS_HOST` | No       | `redis.ser.mlanima.org` | Redis host                                             |

When `DISCORD_CLIENT_ID` or `DISCORD_REDIRECT_URI` are unset, OAuth endpoints
return `400 Discord OAuth is not configured` — graceful degradation.

### Bot Environment Variables

| Variable              | Required | Default       | Description                                         |
| --------------------- | -------- | ------------- | --------------------------------------------------- |
| `DISCORD_TOKEN`       | Yes      | —             | Discord bot token                                   |
| `PLATFORM_API_SECRET` | Yes      | —             | Shared secret (must match `DISCORD_BOT_API_SECRET`) |
| `REDIS_HOST`          | No       | `localhost`   | Redis host                                          |
| `REDIS_PORT`          | No       | `6379`        | Redis port                                          |
| `PORT`                | No       | `3001`        | HTTP server port for internal API                   |
| `LOG_LEVEL`           | No       | `info`        | Pino log level (e.g. `debug`, `warn`)               |
| `NODE_ENV`            | No       | —             | Set to `production` for structured JSON logging     |

### Local Dev

Use `api/.env` (gitignored) with `spring.config.import:optional:file:.env`:

```env
SPRING_DATA_REDIS_HOST=localhost
DISCORD_BOT_BASEURL=http://discord.ser.mlanima.org:3001
DISCORD_BOT_API_SECRET=<shared-secret>
DISCORD_CLIENT_ID=<your-client-id>
DISCORD_CLIENT_SECRET=<your-client-secret>
DISCORD_REDIRECT_URI=http://localhost:8080/api/v1/auth/discord/callback
```

---

## Deployment

| Host                           | Role                 |
| ------------------------------ | -------------------- |
| `mlanima.org` (EC2)            | Discord bot + Redis  |
| `discord.ser.mlanima.org:3001` | Bot internal API     |
| `redis.ser.mlanima.org:6379`   | Redis                |
| `swtp-ss26.de`                 | Spring API + Angular |

Bot deploys automatically via `.github/workflows/ci-cd-bot.yml` on push to
`main`/`developer`. Docker image: `ghcr.io/mlanima/swtp-bot:latest`.

---

## Security

### OAuth2 Callback (CSRF prevention)

- **Self-describing state**: `bot:<nonce>` / `user:<nonce>` prefixes prevent format
  ambiguity — an attacker cannot craft a valid state
- **Server-side nonce**: random UUID, stored in `pendingBotNonces` or `pendingStates`
  with the initiating project/user ID — CSRF-safe
- **Legacy branch protection**: bare-UUID state is only accepted if
  `isPendingBotAuth(projectId)` returns true (pending nonce exists)
- **5-minute TTL**: all nonce maps purged by `@Scheduled` cleanup tasks
- **Guild-only fallback** (callback without `code`): nonce is still validated before
  storing the guild

### Bot Internal API

- Shared `PLATFORM_API_SECRET` / `DISCORD_BOT_API_SECRET` sent as
  `x-internal-secret` header
- **Fail-closed**: `validateSecret()` rejects if secret is missing or env var is unset
- Port 3001 restricted by security group to `144.76.176.84/32` (swtp-ss26.de)

### Token Handling

- Raw OAuth2 token response (containing `access_token`) is **not logged** —
  removed from production logging
- `client_secret` never leaves the server
- Guild data from token exchange is server-authenticated, not from user-facing URL

### Owner Verification

- Bot token exchange: `guild.owner_id` compared against project owner's Discord ID
- Manual connect: checks `project.getOwner().getDiscordId()` is set
- Error message: `"You must be the owner of the Discord server to bind it"`

### Channel Binding Rules

- One guild per project (deactivates old link on reconnect)
- One project per guild (auto-deactivates other projects using same guild)
- Channel cannot be linked to two active projects simultaneously
- Invite URL hidden unless user is project member
