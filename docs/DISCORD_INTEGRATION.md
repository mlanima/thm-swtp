# Discord Integration

## Overview

IdeaCamp integrates with Discord to provide project-connected Discord servers. Each project can link a dedicated Discord channel for posts, automatically creating a `#posts` channel with restricted write access (owner-only posting). Users can link their Discord account via OAuth2 for identity cross-reference and a profile badge.

## Architecture

```
┌─────────────┐     HTTP (shared secret)     ┌──────────────┐
│  Discord    │ ◄──────────────────────────►  │  Discord     │
│  API (Bot)  │  POST /internal/auto-setup    │  Bot         │
│             │  POST /internal/invite        │  (Node.js)   │
│             │  POST /internal/leave-guild   │              │
└──────┬──────┘                               └──────┬───────┘
       │                                              │
       │  Discord                                     │ Redis Streams
       │  Gateway                                     │ (BullMQ)
       ▼                                              ▼
┌─────────────────┐                          ┌──────────────┐
│  Discord.com    │                          │  Redis       │
│  (OAuth2, API)  │                          │  (Streams)   │
└─────────────────┘                          └──────────────┘
       ▲                                              ▲
       │  HTTP (RestTemplate)                         │ spring data redis
       │                                              │
┌──────┴──────────────────────────────────────────────┴──────────┐
│                     Spring Boot API                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Controllers  │  │ Services     │  │ DiscordProperties    │  │
│  │ (Auth,       │──┤ (Channel,    │──│ (config mapping)     │  │
│  │  Channel,    │  │  Auth,       │  │                      │  │
│  │  Settings,   │  │  Notification│  │  bot.base-url        │  │
│  │  Status)     │  │  PostSync)   │  │  oauth.client-id     │  │
│  └─────────────┘  └──────────────┘  └──────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
       ▲
       │ HTTP (REST)
       ▼
┌──────────────────────┐
│  Angular Frontend     │
│  (Profile Badge,      │
│   Project Settings    │
│   Discord Tab)        │
└──────────────────────┘
```

### Components

| Component | Stack | Location |
|-----------|-------|----------|
| **Spring Boot API** | Java 25, Spring Boot 4 | `api/` package `de.thm.swtp.api.discord` |
| **Discord Bot** | Node.js, discord.js v14, BullMQ | `discord-bot/` |
| **Angular Frontend** | Angular 21, PrimeIcons | `web/ideacamp/` |
| **Redis** | Redis Streams + BullMQ | Managed on `redis.ser.mlanima.org:6379` |

### Flows

**Project → Discord Connection Flow:**
1. User links their Discord account via OAuth2 in User Settings
2. User goes to Project Settings → Discord → clicks "Connect Discord Server"
3. API checks user has linked Discord → calls `auto-setup` on bot
4. Bot creates `#posts` channel with permission overwrites (owner + bot can send, @everyone denied)
5. Bot returns guild + channel ID, creates invite → API stores `LinkedChannelEntity`
6. Frontend shows the invite URL and connection status

**Post Sync Flow:**
1. User creates a post in IdeaCamp
2. API publishes event to Redis stream `stream:discord:sync`
3. Bot's BullMQ worker picks up the job
4. Bot sends embed to the linked `#posts` channel

**Disconnect Flow:**
1. User clicks "Disconnect" or the project owner disconnects in Settings
2. API deactivates `LinkedChannelEntity`, calls `POST /internal/leave-guild`
3. Bot leaves the Discord guild

---

## Backend — REST API

### Discord Auth (`/api/v1/auth/discord`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/authorize` | ✅ | Returns Discord OAuth2 URL (redirects user to Discord) |
| `GET` | `/callback` | ❌ | OAuth2 callback — exchanges code, links Discord account |
| `DELETE` | `/disconnect` | ✅ | Unlinks Discord account from user profile |

### Discord Channel (`/api/v1/projects/{projectId}/discord`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/connect` | ✅ | Manually link channel by ID |
| `GET` | `/bot-invite` | ✅ | Get bot invite URL |
| `POST` | `/auto-connect` | ✅ | Auto-create `#posts` via bot |
| `PATCH` | `/invite` | ✅ | Refresh/create Discord invite |
| `DELETE` | `/connect` | ✅ | Disconnect (deactivate + bot leaves guild) |
| `GET` | `/connect` | ✅ | Get current connection status |

### Discord Settings (`/api/v1/projects/{projectId}/discord/settings`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/` | ✅ | Get channel settings |
| `PUT` | `/` | ✅ | Update channel settings |

### Discord Status (`/api/v1/projects/{projectId}/discord/status`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/` | ✅ | Get Discord connection status |

### Bot Internal API (not exposed publicly)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/internal/auto-setup` | Create `#posts` channel with permission overwrites |
| `POST` | `/internal/channels/:channelId/invite` | Create invite for channel |
| `POST` | `/internal/channels/:channelId/leave-guild` | Bot leaves the guild |
| `POST` | `/internal/channels/:channelId/restrict` | Restrict channel to owner-only |
| `GET` | `/health` | Health check (used by monitoring) |

---

## Backend — Key Classes

| Class | Purpose |
|-------|---------|
| `DiscordAuthController` | OAuth2 redirect + callback endpoints |
| `DiscordChannelController` | Channel connect/disconnect/auto-setup |
| `DiscordAuthService` | OAuth2 state management, token exchange, linking |
| `DiscordChannelService` | Channel linking logic, one-server-per-project enforcement |
| `DiscordStatusService` | Connection status queries |
| `DiscordNotificationService` | Sends post sync events to Redis |
| `DiscordPostSyncService` | Processes synced posts from Discord |
| `BotInternalClient` | HTTP client for bot internal API (shared secret auth) |
| `DiscordOAuthClient` | HTTP client for Discord OAuth2 token + user info |
| `DiscordProperties` | `@ConfigurationProperties` mapping for `discord.*` |
| `DiscordStreamConfig` | Redis Stream configuration |
| `LinkedChannelEntity` | JPA entity: project ↔ Discord channel mapping |
| `DiscordChannelSettingsEntity` | JPA entity: per-channel settings |
| `DiscordMessageSyncEntity` | JPA entity: synced message tracking |

---

## Frontend — Components

| Component | Route / Location | Purpose |
|-----------|-----------------|---------|
| `user-settings/discord-tab` | User Settings → Discord | Link/unlink Discord account, show status |
| `project-settings/discord-tab` | Project Settings → Discord | Connect/disconnect server, invite link |
| `profile-banner` | User profile page | Discord badge with copy-to-clipboard |

### Profile Badge Behavior

1. **Initial state**: Small pill showing Discord icon
2. **Click 1**: Reveals username + switches to outlined style
3. **Click 2**: Copies `username#tag` to clipboard → shows "Copied!" for 2s → resets

### Auto-Connect on Tab Return

When the browser tab regains focus after the Discord OAuth redirect, the `discord-tab` component checks for a `discord-pending-setup` flag and triggers `autoConnect()` automatically.

---

## Discord Bot (`discord-bot/`)

| File | Purpose |
|------|---------|
| `index.ts` | Entry point, starts Discord client + worker + HTTP server |
| `bot/client.ts` | Discord.js client setup with intents |
| `bot/handlers/*.ts` | Event handlers (channelDelete, messageDelete, messageCreate, etc.) |
| `http/internalApi.ts` | Express server with internal endpoints for Spring API |
| `streams/consumer.ts` | BullMQ worker for consuming Redis streams |
| `streams/producer.ts` | BullMQ producer for Discord → API events |
| `streams/eventHandler.ts` | Maps stream events to Discord actions |
| `streams/validator.ts` | Validates event payloads against types |
| `queues/discordQueue.ts` | BullMQ queue definitions |
| `config/redis.ts` | Redis connection via ioredis |
| `config/logger.ts` | Pino logger |
| `metrics/index.ts` | Prometheus metrics endpoint |

### BullMQ Queues

| Queue | Purpose |
|-------|---------|
| `sendPost` | Send embed message to Discord channel |
| `deletePost` | Delete message from Discord channel |

---

## Redis Streams

Two Redis streams handle async communication between the API and bot:

| Stream | Direction | Payload |
|--------|-----------|---------|
| `stream:discord:sync` | API → Bot | Post created/updated/deleted events |
| `stream:platform:sync` | Bot → API | Discord events (message delete, channel updates) |

The API uses Spring Data Redis (`DiscordEventPublisher`). The bot uses BullMQ for reliable processing with retries.

---

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DISCORD_BOT_BASEURL` | No | `http://localhost:3001` | Bot internal API URL |
| `DISCORD_BOT_API_SECRET` | No | (empty) | Shared secret for bot ↔ API auth |
| `DISCORD_CLIENT_ID` | No | (empty) | Discord OAuth2 app client ID |
| `DISCORD_CLIENT_SECRET` | No | (empty) | Discord OAuth2 app client secret |
| `DISCORD_REDIRECT_URI` | No | (empty) | OAuth2 callback URL |
| `SPRING_DATA_REDIS_HOST` | No | `redis.ser.mlanima.org` | Redis host for streams |

When `DISCORD_CLIENT_ID` or `DISCORD_REDIRECT_URI` are empty/blank, the OAuth endpoints return `400 Discord OAuth is not configured` — allowing the app to start without Discord.

### Discord Bot Env Vars

| Variable | Required | Description |
|----------|----------|-------------|
| `DISCORD_TOKEN` | Yes | Discord bot token |
| `DISCORD_CLIENT_ID` | Yes | Bot application ID |
| `REDIS_HOST` | Yes | Redis host (set to `127.0.0.1` or private IP) |
| `REDIS_PORT` | No | Redis port (default `6379`) |
| `PLATFORM_API_BASEURL` | Yes | Spring API base URL |
| `PLATFORM_API_SECRET` | Yes | Shared secret (must match `DISCORD_BOT_API_SECRET`) |
| `PORT` | No | HTTP server port (default `3001`) |

### Local Development

Use the `api/.env` file (gitignored) with `spring.config.import: optional:file:.env`:

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

### Infrastructure

| Host | Role |
|------|------|
| `mlanima.org` (EC2) | Discord bot server + Redis |
| `discord.ser.mlanima.org:3001` | Bot internal API (accessible only from swtp-ss26.de) |
| `redis.ser.mlanima.org:6379` | Redis (on same EC2) |
| `swtp-ss26.de` | Spring API + Angular (Docker Compose stacks) |

### CI/CD

Bot auto-deploys on push to `main`/`developer` via `.github/workflows/ci-cd-bot.yml`.

### Docker

Bot image: `ghcr.io/mlanima/swtp-bot:latest` (or `:dev`)

```yaml
# docker-compose.yml (bot)
services:
  discord-bot-main:
    image: ghcr.io/${GHCR_NAMESPACE:-mlanima}/swtp-bot:latest
    ports:
      - "3001:3001"
    env_file: .env.main
    restart: unless-stopped
```

---

## Security

- **Bot Internal API**: Authenticated via shared `PLATFORM_API_SECRET` header on every request
- **Discord OAuth2**: Standard PKCE-less OAuth2 with state parameter for CSRF protection
- **One server per project**: `connectChannel()` rejects if channel/guild already linked to another active project
- **Owner must link Discord**: `DiscordConnectionFailedException` thrown if `project.getOwner().getDiscordId()` is null
- **Invite URL visibility**: `discordInviteUrl` only included in response for project owner/members (checked via `SecurityContextHolder`)
- **Security group**: Port 3001 restricted to `144.76.176.84/32` (swtp-ss26.de)
- **`application.yaml`**: No hardcoded secrets — all via required env vars with empty defaults
- **OAuth graceful degradation**: When `clientId` is unconfigured, endpoints return `400` instead of crashing
