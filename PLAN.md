# Discord Integration — Implementation Plan

**Based on:** `discord-integration.md`
**Stack:** Spring Boot 4.0.6 (Java 25) · Node.js (TypeScript) + discord.js v14 · Redis Streams · Angular 21
**Package (backend):** `de.thm.swtp.api.discord`

---

## Overview

This plan implements the Discord integration in a single iteration (6 workstreams, sequential+parallel).

### Dependencies map

```
Ticket 1 ──┬── Ticket 2 (parallel after T1)
           └── Ticket 3 (parallel after T1)
                    │
                    ├── Ticket 4 (needs T3)
                    └── Ticket 5 (needs T3, overlaps T4)
                             │
                    Ticket 6 (needs T4+T5, resilience only, no dashboard UI)
```

---

## Workstream 1 — Foundation: Schema, Config, Bot Scaffold

### DB entities

New entities under `de.thm.swtp.api.discord.entity`:

| Entity                         | Table                      | Notes                                                          |
| ------------------------------ | -------------------------- | -------------------------------------------------------------- |
| `LinkedChannelEntity`          | `linked_channels`          | Maps project → Discord channel. Unique on `project_id`.        |
| `DiscordMessageSyncEntity`     | `discord_message_sync`     | Post ↔ Discord message. Unique on `discord_message_id`.        |
| `DiscordChannelSettingsEntity` | `discord_channel_settings` | Per-channel notification flags. Unique on `linked_channel_id`. |

**Modify** `UserProfileEntity` (`api/src/main/java/.../userprofile/entity/UserProfile.java`):

- Add `discordId` (`VARCHAR(20)`, unique, nullable)
- Add `discordUsername` (`VARCHAR(50)`, nullable)
- Add `discordConnectedAt` (`LocalDateTime`, nullable)

### Redis Stream config

New package: `de.thm.swtp.api.discord.config`

- `DiscordProperties` — `@ConfigurationProperties(prefix = "discord")` binding `application.yaml` entries for stream names, consumer group, batch size, block-ms, pending-timeout-ms, bot base-url, api-secret
- `DiscordStreamConfig` — dedicated `TaskScheduler` bean (pool size 4, `discord-sync-` prefix) so blocking stream reads don't starve other `@Scheduled` tasks
- Consumer name: `spring-worker-${hostname}-${pid}` (computed at startup)

### Repositories

Under `de.thm.swtp.api.discord.repository`:

| Repository                         | Key methods                                                                                             |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `LinkedChannelRepository`          | `findByProjectId(UUID)`, `findByChannelId(String)`, `findByChannelIdAndIsActiveTrue(String)`            |
| `DiscordMessageSyncRepository`     | `existsByDiscordMessageId(String)`, `findByPlatformPostId(UUID)`, `countBySyncedAtAfter(LocalDateTime)` |
| `DiscordChannelSettingsRepository` | `findByLinkedChannelId(UUID)`                                                                           |

### `application.yaml` additions

```yaml
discord:
    streams:
        outbound: stream:discord:sync
        inbound: stream:platform:sync
        consumer-group: spring-platform
        batch-size: 10
        block-ms: 5000
        pending-timeout-ms: 30000
    bot:
        base-url: http://discord.ser.mlanima.org:3001   # hardcoded for testing; switch to ${BOT_INTERNAL_URL} in production
        api-secret: ${PLATFORM_API_SECRET}
```

### Node.js bot scaffold

New directory `discord-bot/` at repo root:

```
discord-bot/
├── src/
│   ├── index.ts                  # entry (logs Ready on start)
│   ├── bot/
│   │   ├── client.ts             # discord.js client init (intents: Guilds, GuildMessages, MessageContent, DirectMessages)
│   │   └── handlers/             # (empty handlers scaffolded, wired in index.ts)
│   ├── streams/
│   │   ├── consumer.ts           # scaffold
│   │   ├── producer.ts           # scaffold
│   │   └── validator.ts          # zod schemas for stream event payloads
│   ├── queues/
│   │   └── discordQueue.ts       # scaffold
│   ├── http/
│   │   └── internalApi.ts        # scaffold (Express router)
│   ├── metrics/
│   │   └── index.ts              # prom-client counters & histogram, GET /metrics
│   ├── types/
│   │   └── events.ts             # shared TypeScript types for stream events
│   └── config/
│       ├── redis.ts              # ioredis connection
│       └── logger.ts             # pino logger instance
├── __tests__/                    # vitest test files
├── package.json                  # discord.js@14, bullmq, ioredis, express, pino, zod, vitest, prom-client, typescript, tsx, @types/*
├── tsconfig.json                 # strict mode, ESM, path aliases
├── Dockerfile
└── .env.example
```

### Bootstrap (npm)

```bash
mkdir discord-bot
cd discord-bot
npm init -y
npm install discord.js bullmq ioredis express pino zod prom-client
npm install --save-dev typescript tsx @types/node @types/express vitest
npx tsc --init --target ES2022 --module nodenext --strict true --outDir dist --rootDir src
```

Then create the directory tree:

```bash
mkdir -p src/{bot/handlers,streams,queues,http,metrics,types,config} __tests__
```

This produces the structure shown above. The `package.json` must be edited to add `"type": "module"` and the `scripts` block:

```json
{
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "test": "vitest"
  }
}
```

### Files to create/modify

| File                                                                             | Action                                  |
| -------------------------------------------------------------------------------- | --------------------------------------- |
| `api/src/main/java/.../discord/entity/LinkedChannelEntity.java`                  | Create                                  |
| `api/src/main/java/.../discord/entity/DiscordMessageSyncEntity.java`             | Create                                  |
| `api/src/main/java/.../discord/entity/DiscordChannelSettingsEntity.java`         | Create                                  |
| `api/src/main/java/.../discord/repository/LinkedChannelRepository.java`          | Create                                  |
| `api/src/main/java/.../discord/repository/DiscordMessageSyncRepository.java`     | Create                                  |
| `api/src/main/java/.../discord/repository/DiscordChannelSettingsRepository.java` | Create                                  |
| `api/src/main/java/.../discord/config/DiscordProperties.java`                    | Create                                  |
| `api/src/main/java/.../discord/config/DiscordStreamConfig.java`                  | Create                                  |
| `api/src/main/java/.../userprofile/entity/UserProfile.java`                      | Modify — add 3 discord fields           |
| `api/src/main/resources/application.yaml`                                        | Modify — add `discord.*` config section |
| `discord-bot/` (entire tree incl. `tsconfig.json`, `__tests__/`)                | Create                                  |

---

## Workstream 2 — Account & Channel Linking

### Discord OAuth2 (backend)

New controller + service + client:

- `DiscordAuthController` (`/api/auth/discord/authorize`, `/api/auth/discord/callback`, `/api/auth/discord/disconnect`)
- `DiscordAuthService` — OAuth2 logic, state param validation, CSRF protection
- `DiscordOAuthClient` — exchanges auth code for token, calls Discord `/users/@me`

Flow:

1. `GET /authorize` → redirects to Discord with `state` (random, stored in session/redis)
2. `GET /callback?code=&state=` → validates state, exchanges code, stores `discordId`/`discordUsername`/`discordConnectedAt` on `UserProfile`
3. 409 if `discordId` already linked to another account
4. `DELETE /disconnect` → clears discord fields, deactivates all `LinkedChannel` rows where this user is the project owner

### Channel linking (backend)

- `DiscordChannelController` (`POST/DELETE /api/projects/{projectId}/discord/connect`, `GET/PUT /api/projects/{projectId}/discord/settings`)
- `DiscordChannelService` — creates/deactivates links, manages settings
- `BotInternalClient` — Spring `RestClient` → bot's `POST /internal/test-connection` (authenticated with `x-internal-secret` header)

Flow:

1. Admin inputs Discord channel ID on project settings page
2. Spring calls `POST /internal/test-connection` on bot to verify bot has access
3. If success → create `LinkedChannelEntity` + default `DiscordChannelSettingsEntity`
4. If failure → return 400 with reason from bot

### Frontend

**User settings** — add a `discord-tab/` under `web/ideacamp/src/app/feature/user-settings/tabs/`:

- "Connect Discord" button → redirects to `/api/auth/discord/authorize`
- Shows connected Discord username when linked
- "Disconnect" button

**Project settings** — add a `discord-tab/` under `web/ideacamp/src/app/feature/project-settings/tabs/`:

- Channel ID input + "Test & Connect" button
- Shows connected channel ID, connection status
- "Disconnect" button (sets `is_active = false`)
- Notification settings toggles (delegated from Workstream 5, but UI built here)

### Files to create/modify

| File                                                                | Action                                    |
| ------------------------------------------------------------------- | ----------------------------------------- |
| `api/.../discord/controller/DiscordAuthController.java`             | Create                                    |
| `api/.../discord/controller/DiscordChannelController.java`          | Create                                    |
| `api/.../discord/controller/DiscordSettingsController.java`         | Create                                    |
| `api/.../discord/service/DiscordAuthService.java`                   | Create                                    |
| `api/.../discord/service/DiscordChannelService.java`                | Create                                    |
| `api/.../discord/client/DiscordOAuthClient.java`                    | Create                                    |
| `api/.../discord/client/BotInternalClient.java`                     | Create                                    |
| `discord-bot/src/http/internalApi.ts`                               | Create — `POST /internal/test-connection` |
| `web/ideacamp/src/app/feature/user-settings/tabs/discord-tab/`      | Create                                    |
| `web/ideacamp/src/app/feature/project-settings/tabs/discord-tab/`   | Create                                    |
| `web/ideacamp/src/app/feature/user-settings/user-settings.ts`       | Modify — add discord tab                  |
| `web/ideacamp/src/app/feature/project-settings/project-settings.ts` | Modify — add discord tab                  |

---

## Workstream 3 — Stream Infrastructure

### Spring → Node.js (`stream:discord:sync`)

**`DiscordEventPublisher`** (`de.thm.swtp.api.discord.stream`):

| Method                                             | Event type       | When                                                                 |
| -------------------------------------------------- | ---------------- | -------------------------------------------------------------------- |
| `publishPostCreated(Post)`                         | `POST_CREATED`   | Post published                                                       |
| `publishPostUpdated(Post, discordMsgId)`           | `POST_UPDATED`   | Post edited, only if sync mapping exists                             |
| `publishPostDeleted(postId, discordMsgId)`         | `POST_DELETED`   | Post deleted, only if sync mapping exists                            |
| `publishProjectInvite(Invite)`                     | `PROJECT_INVITE` | Invite created, only if recipient has `discordId`                    |
| `publishProjectEvent(eventType, project, message)` | `PROJECT_EVENT`  | Member joined / status changed, only if active linked channel exists |

All methods:

- Check for active `LinkedChannel` first (or relevant condition), silently skip if absent
- Truncate content to 3,900 chars (headroom under Discord's 4,096 embed limit)
- Write via `redis.opsForStream().add(outboundStream, fields)`

### Node.js → Spring (`stream:platform:sync`)

**`PlatformSyncConsumer`** (`de.thm.swtp.api.discord.stream`):

- `@PostConstruct` — idempotent consumer group creation (catch `BUSYGROUP`)
- `@Scheduled(fixedDelay = 100)` — batch `XREADGROUP` with `Consumer.from(consumerGroup, consumerName)`
- Processes: `DISCORD_MESSAGE_ASSIGNED`, `DISCORD_MESSAGE_CREATED`, `DISCORD_MESSAGE_UPDATED`, `DISCORD_MESSAGE_DELETED`, `INVITE_RESPONSE`, `CHANNEL_DISCONNECTED`
- `@Transactional` on `handleDiscordMessageCreated` and `handleMessageAssigned`
- `@Scheduled(fixedDelay = 30000)` — `reclaimPending()` iterates pending entries past `pendingTimeoutMs`, claims and reprocesses them
- `XACK` only after successful processing; un-ACKed messages remain for retry

### Node.js stream consumer + producer

**`consumer.ts`** — infinite loop `XREADGROUP`/`XAUTOCLAIM`, validates payloads with Zod, routes to BullMQ:

- Routes `POST_CREATED` → job `sendPost` (5 attempts)
- Routes `POST_UPDATED` → job `editPost` (3 attempts)
- Routes `POST_DELETED` → job `deletePost` (3 attempts)
- Routes `PROJECT_INVITE` → job `sendInvite` (3 attempts)
- Routes `PROJECT_EVENT` → job `sendEvent` (3 attempts)
- All payloads validated through `validator.ts` Zod schemas before enqueue — malformed messages are logged (pino) and `XACK`ed without creating a job
- `XACK` once the job is enqueued (not after execution)

**`producer.ts`** — `streamProducer` object with methods:

- `discordMessageAssigned({ postId, discordMsgId, channelId })`
- `discordMessageCreated({ discordMsgId, channelId, content, discordUserId, discordUsername, attachmentUrls })`
- `discordMessageUpdated({ discordMsgId, content })`
- `discordMessageDeleted({ discordMsgId })`
- `inviteResponse({ inviteId, response })`
- `channelDisconnected({ channelId, reason })`

### BullMQ worker

**`discordQueue.ts`** — `Queue` + `Worker` for `discord-actions`:

- Exponential backoff: 15s, 30s, 60s, 120s, 240s cap (Discord's `retryAfter` honored on 429)
- All sends use `allowed_mentions: { parse: [] }` to prevent `@everyone`/`@here` abuse
- Content length guards (embed ≤ 4096, message ≤ 2000) before sending

### Files to create

| File                         | Location                   |
| ---------------------------- | -------------------------- |
| `DiscordEventPublisher.java` | `api/.../discord/stream/`  |
| `PlatformSyncConsumer.java`  | `api/.../discord/stream/`  |
| `consumer.ts`                | `discord-bot/src/streams/` |
| `producer.ts`                | `discord-bot/src/streams/` |
| `validator.ts`               | `discord-bot/src/streams/` |
| `eventHandler.ts`            | `discord-bot/src/streams/` |
| `discordQueue.ts`            | `discord-bot/src/queues/`  |

---

## Workstream 4 — Bidirectional Post Sync

### Platform → Discord

**Modify** `ProjectPostService`:

- `createProjectPost()` / `publishProjectPost()` → call `DiscordEventPublisher.publishPostCreated()` after save
- `archiveProjectPost()` → call `publishPostUpdated()` if sync mapping exists (content visible again)
- `deleteProjectPost()` → call `publishPostDeleted()` if sync mapping exists

**Bot's BullMQ worker** (`sendPost` job):

1. Fetch channel, send embed (author name, avatar, content, platform URL)
2. On success → `streamProducer.discordMessageAssigned({ postId, discordMsgId, channelId })`
3. Write Redis dedup key: `redis.set(`dedup:discord:msg:${message.id}`, '1', 'EX', 86400)`
4. `editPost` job — fetches channel+message, updates embed description
5. `deletePost` job — deletes message, ignores error 10008 (already gone)

### Discord → Platform

**Bot handlers**:

- `messageCreate.ts` — skip if `author.bot || webhookId`, check linked channel set, check Redis dedup key → produce `DISCORD_MESSAGE_CREATED`
- `messageUpdate.ts` — same guards → produce `DISCORD_MESSAGE_UPDATED`
- `messageDelete.ts` — same guards → produce `DISCORD_MESSAGE_DELETED`

**Spring consumer** handles:

- `DISCORD_MESSAGE_CREATED` → resolve project via `channelId → LinkedChannel`, find/create author via `discordId`, create post via `ProjectPostService.createFromDiscord()`, save sync mapping
- `DISCORD_MESSAGE_UPDATED` → find post via sync mapping, update content
- `DISCORD_MESSAGE_DELETED` → find post via sync mapping, delete post

### Duplicate protection (per §7)

| Layer | Where                                | What                                                                                                                                     |
| ----- | ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | Bot `messageCreate`                  | `author.bot` check — catches own messages before any processing                                                                          |
| 2     | Bot `messageCreate`                  | Redis key `dedup:discord:msg:{id}` (24h TTL) — fast first filter                                                                         |
| 3     | Spring `handleDiscordMessageCreated` | DB unique constraint on `discord_message_id` in `discord_message_sync` — durable guard, transaction rolls back post creation on conflict |

### Files to create/modify

| File                                                  | Action                                          |
| ----------------------------------------------------- | ----------------------------------------------- |
| `api/.../projectPost/service/ProjectPostService.java` | Modify — add publisher calls                    |
| `api/.../discord/service/DiscordPostSyncService.java` | Create — helper for Discord→Post mapping        |
| `api/.../discord/stream/PlatformSyncConsumer.java`    | Modify — implement handler logic                |
| `discord-bot/src/bot/handlers/messageCreate.ts`       | Create                                          |
| `discord-bot/src/bot/handlers/messageUpdate.ts`       | Create                                          |
| `discord-bot/src/bot/handlers/messageDelete.ts`       | Create                                          |
| `discord-bot/src/queues/discordQueue.ts`              | Modify — implement sendPost/editPost/deletePost |

---

## Workstream 5 — Invites & Project Notifications

### Discord DM invites

**Modify** `ProjectInviteService.createProjectInvite()`:

- After saving invite, check `invitedUser.getDiscordId()`
- If present → call `DiscordEventPublisher.publishProjectInvite(invite)`

**Bot** (`sendInvite` job):

1. Fetch user by `targetDiscordId`
2. Send DM with embed (project name, inviter) + ActionRow with Accept/Decline buttons
3. DM blocked (error 50007) → log warning, do not throw

**Bot** (`interactionCreate.ts`):

- Parse `invite_accept_{id}` / `invite_decline_{id}` custom IDs
- Produce `INVITE_RESPONSE` with response = `ACCEPTED` / `DECLINED`
- Update interaction message to show outcome, remove buttons

**Spring consumer** (`handleInviteResponse`):

- Call `inviteService.processResponse(inviteId, accepted)` — reuses existing `updateInviteStatus`

### Project event notifications

**Modify** `ProjectService` (member add/remove) and related services:

- Publish `PROJECT_EVENT` via `DiscordEventPublisher.publishProjectEvent()`
- Gated by `DiscordChannelSettings` — check per-event-type flag before publishing

**Bot** (`sendEvent` job):

- Send simple embed to channel (neutral color `0x99AAB5`, no author)
- Never written to `discord_message_sync` — events don't sync back

### Notification settings API

| Endpoint                                     | Method | Purpose                     |
| -------------------------------------------- | ------ | --------------------------- |
| `/api/projects/{projectId}/discord/settings` | GET    | Get notification toggles    |
| `/api/projects/{projectId}/discord/settings` | PUT    | Update notification toggles |

Frontend: toggle switches in project settings → discord tab.

### Files to create/modify

| File                                                          | Action                                                |
| ------------------------------------------------------------- | ----------------------------------------------------- |
| `api/.../projectInvitation/service/ProjectInviteService.java` | Modify — add Discord check + publish call             |
| `api/.../project/ProjectService.java`                         | Modify — publish member join/status events            |
| `api/.../projectInvitation/domain/ProjectInvite.java`         | Check if `discordId` accessor exists on domain object |
| `api/.../discord/service/DiscordNotificationService.java`     | Create — event notification logic                     |
| `discord-bot/src/bot/handlers/interactionCreate.ts`           | Create                                                |
| `discord-bot/src/queues/discordQueue.ts`                      | Modify — implement sendInvite/sendEvent               |

---

## Workstream 6 — Operational Resilience (No Admin Dashboard)

### Bot disconnection detection

**Bot handlers**:

- `channelDelete.ts` — on channel in linked set → `streamProducer.channelDisconnected({ channelId, reason: 'CHANNEL_DELETED' })`
- `guildBanAdd.ts` — check if bot itself was banned → `streamProducer.channelDisconnected` for all linked channels in that guild

**Spring consumer** (`handleChannelDisconnected`):

- Set `linked_channels.is_active = false` for matching channel
- Trigger in-app notification to project owner
- _(Dashboard banner and UI status display deferred)_

### Job cancellation for dead channels

**BullMQ worker enhancement**:

- Before sending to a channel, check if channel is still active via a Redis set of active channels (maintained by bot)
- If inactive → fail job immediately with "channel disconnected" reason

### Test connection & retry (internal HTTP API)

**Bot** `internalApi.ts`:

- `POST /internal/test-connection` — fetch channel, send test embed, return JSON `{ success, reason }`
- `POST /internal/jobs/:jobId/retry` — `bullmq` `job.retry()`, return `{ success }`
- Both guarded by `x-internal-secret` header match

### Health & metrics

**Bot** — new endpoints in `internalApi.ts`:

- `GET /health` — lightweight liveness check (Redis ping, Discord WS status), returns `{ status: "ok", discord: "ready"|"reconnecting", redis: "up"|"down" }`
- `GET /metrics` — Prometheus scrape endpoint via `prom-client`, exposing:
  - `discord_events_processed_total` (counter, labeled by event type)
  - `discord_messages_sent_total` (counter, status label)
  - `discord_redis_stream_lag` (gauge)
  - `discord_queue_depth` (gauge, pulled from BullMQ)

Both guarded by `x-internal-secret` header.

**Notes:**

- `prom-client` metrics are registered in a shared registry (`metrics/index.ts`) and populated by stream consumer, BullMQ worker, and bot event handlers as they execute.
- No separate health infrastructure during this iteration — endpoints are consumed by Docker HEALTHCHECK and/or orchestrator probes. Integration with the dashboard is deferred.

**Spring** `BotInternalClient`:

- `testConnection(channelId)` → calls bot, returns `{ success, reason }`
- `retryJob(jobId)` → calls bot, returns `{ success }`

### Status endpoint (API-only)

- `GET /api/projects/{projectId}/discord/status` → returns `{ isActive, channelId, syncedToday, failedToday }`
- Counts from `DiscordMessageSyncRepository` (syncedToday) and BullMQ failed jobs (via bot's internal API or direct Redis)
- _(No frontend for this — API available for future dashboard)_

### Files to create/modify

| File                                                      | Action                                         |
| --------------------------------------------------------- | ---------------------------------------------- |
| `api/.../discord/controller/DiscordStatusController.java` | Create                                         |
| `api/.../discord/service/DiscordStatusService.java`       | Create                                         |
| `api/.../discord/client/BotInternalClient.java`           | Modify — add `testConnection()` + `retryJob()` |
| `discord-bot/src/bot/handlers/channelDelete.ts`           | Create                                         |
| `discord-bot/src/bot/handlers/guildBanAdd.ts`             | Create                                         |
| `discord-bot/src/http/internalApi.ts`                     | Modify — add retry, health & metrics endpoints |
| `discord-bot/src/metrics/index.ts`                        | Create                                         |

---

## Workstream 7 — CI/CD & Deployment

### W7.1 — Bot Dockerfile

Multi-stage build (`discord-bot/Dockerfile`):

```dockerfile
# Builder
FROM node:24-alpine AS builder
WORKDIR /app
COPY package*.json tsconfig.json ./
RUN npm ci
COPY src/ src/
RUN npm run build

# Runtime
FROM node:24-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/dist dist/
COPY package*.json ./
RUN npm ci --omit=dev
USER app
EXPOSE 3001
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3001/health || exit 1
CMD ["node", "dist/index.js"]
```

### W7.2 — Docker Compose stack (Server B)

New directory `infra/ser.mlanima.org/discord-bot/`:

```
infra/ser.mlanima.org/discord-bot/
├── .env.main.example
├── .env.dev.example
├── docker-compose.yml
└── deploy.bb
```

**`docker-compose.yml`:**

```yaml
services:
  discord-bot-main:
    image: ghcr.io/${GHCR_NAMESPACE:-mlanima}/swtp-bot:latest
    ports:
      - "127.0.0.1:3001:3001"
    env_file: .env.main
    networks:
      - bot_internal
    restart: unless-stopped

  discord-bot-dev:
    image: ghcr.io/${GHCR_NAMESPACE:-mlanima}/swtp-bot:dev
    ports:
      - "127.0.0.1:3002:3001"
    env_file: .env.dev
    networks:
      - bot_internal
    restart: unless-stopped

networks:
  bot_internal:
    driver: bridge
```

**`.env.main.example`** / **`.env.dev.example`**:

```
DISCORD_TOKEN=<discord-bot-token>
REDIS_HOST=redis
REDIS_PORT=6379
PLATFORM_API_SECRET=<shared-secret-with-spring>
PORT=3001
```

The bot connects to the existing Redis on the same server (`redis.ser.mlanima.org`).

### W7.3 — GitHub Actions workflow

New file `.github/workflows/ci-cd-bot.yml`:

| Trigger | Event | Action |
|---|---|---|
| PR to `main` / `developer` | `opened`, `synchronize` | CI: `npm ci`, `npm run build`, `npm test` |
| Push to `main` | `push` | Build + push `ghcr.io/*/swtp-bot:latest` → SSH deploy to Server B |
| Push to `developer` | `push` | Build + push `ghcr.io/*/swtp-bot:dev` → SSH deploy to Server B |
| Path filter | | Only when `discord-bot/**` changes |

**SSH deploy step:**

```yaml
- name: Deploy bot
  uses: appleboy/ssh-action@v1
  with:
    host: ser.mlanima.org
    username: <user>
    key: ${{ secrets.BOT_DEPLOY_SSH_KEY }}
    script: |
      cd /opt/stacks/discord-bot
      docker compose pull
      docker compose up -d
```

### W7.4 — Spring → Bot connectivity (testing phase)

For initial testing, `BOT_INTERNAL_URL` is **hardcoded** in `application.yaml`:

```yaml
discord:
  bot:
    base-url: http://discord.ser.mlanima.org:3001
    api-secret: ${PLATFORM_API_SECRET}
```

Spring on Server A calls the bot over plain HTTP. The `x-internal-secret` header guards all endpoints. When promoting to production, replace the hardcoded URL with `${BOT_INTERNAL_URL}`.

No Traefik routing, no TLS termination, no changes to the swtp server — Server A only needs outbound HTTP access to port 3001/3002 on `ser.mlanima.org`.

### W7.5 — Firewall (Server B)

Open ports **3001** and **3002** for TCP inbound from Server A's IP only (Spring → bot). All other inbound traffic denied.

### Files to create/modify

| File | Action |
|---|---|
| `discord-bot/Dockerfile` | Create (multi-stage) |
| `infra/ser.mlanima.org/discord-bot/docker-compose.yml` | Create |
| `infra/ser.mlanima.org/discord-bot/.env.main.example` | Create |
| `infra/ser.mlanima.org/discord-bot/.env.dev.example` | Create |
| `infra/ser.mlanima.org/discord-bot/deploy.bb` | Optional — create if upgrading to Babashka |
| `.github/workflows/ci-cd-bot.yml` | Create |

---

## Package structure summary (backend)

```
de.thm.swtp.api.discord/
├── config/
│   ├── DiscordProperties.java
│   └── DiscordStreamConfig.java
├── entity/
│   ├── LinkedChannelEntity.java
│   ├── DiscordMessageSyncEntity.java
│   └── DiscordChannelSettingsEntity.java
├── repository/
│   ├── LinkedChannelRepository.java
│   ├── DiscordMessageSyncRepository.java
│   └── DiscordChannelSettingsRepository.java
├── stream/
│   ├── DiscordEventPublisher.java
│   └── PlatformSyncConsumer.java
├── service/
│   ├── DiscordAuthService.java
│   ├── DiscordChannelService.java
│   ├── DiscordPostSyncService.java
│   ├── DiscordNotificationService.java
│   └── DiscordStatusService.java
├── client/
│   ├── DiscordOAuthClient.java
│   └── BotInternalClient.java
└── controller/
    ├── DiscordAuthController.java
    ├── DiscordChannelController.java
    ├── DiscordSettingsController.java
    └── DiscordStatusController.java
```

## Environment variables

| Variable                | Used by      | Purpose                            |
| ----------------------- | ------------ | ---------------------------------- |
| `REDIS_HOST`            | Spring       | Redis host                         |
| `REDIS_PORT`            | Spring       | Redis port (default 6379)          |
| `REDIS_PASSWORD`        | Spring       | Redis password                     |
| `BOT_INTERNAL_URL`      | Spring       | Bot HTTP API base URL (optional — hardcoded in `application.yaml` for testing; use env var in production) |
| `PLATFORM_API_SECRET`   | Spring + Bot | Shared secret for Spring→Bot calls |
| `DISCORD_CLIENT_ID`     | Spring       | Discord OAuth2 client ID           |
| `DISCORD_CLIENT_SECRET` | Spring       | Discord OAuth2 client secret       |
| `DISCORD_REDIRECT_URI`  | Spring       | OAuth2 callback URL                |
| `DISCORD_TOKEN`         | Bot          | Discord bot token                  |
| `REDIS_URL`             | Bot          | Redis connection string            |
| `PORT`                  | Bot          | Internal HTTP port (default 3001)  |

## Deferred (not in this iteration)

- Admin dashboard UI (status cards, counter display, retry buttons, "disconnected" banner)
- Visual connection status on project page frontend
- `GET /api/projects/{projectId}/discord/status` frontend integration
