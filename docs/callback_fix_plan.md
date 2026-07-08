# Discord Callback Fix — Plan

## Problem

The OAuth callback for bot authorization receives `?code=...&state=...` but never `guild_id` in the query string. Discord only guarantees guild info in the **token exchange response body** (not the redirect URL), and only when the app has **"Requires OAuth2 Code Grant"** enabled.

## Precondition

"Requires OAuth2 Code Grant" is **enabled** in the Discord Developer Portal → Bot settings. This forces every bot install through a full authorization code grant flow, making the token exchange response include the `guild` object.

## How it works

1. **Invite URL**: `scope=bot%20identify&response_type=code&redirect_uri=...&state=bot:<nonce>`
2. **State**: `bot:<nonce>` where nonce is a random UUID. Server stores `nonce → { projectId, userId, expiry }`.
3. **Callback receives**: `?code=...&state=bot:<nonce>` (no `guild_id`)
4. **Flow detection**: Parse `state` prefix — `bot:` → bot binding, `user:` → user login, other → reject
5. **Token exchange**: POST `code` to Discord's `/oauth2/token` with `client_id`, `client_secret`, `grant_type=authorization_code`, `redirect_uri`
6. **Token response** includes:
   ```json
   {
     "access_token": "...",
     "guild": { "id": "...", "name": "...", "owner_id": "..." },
     "scope": "bot identify"
   }
   ```
7. `guild.id` is read from the response body — cryptographically tied to this specific authorization
8. **Ownership check**: `guild.owner_id` must match the project owner's Discord ID
9. **Already-bound check**: guild must not be linked to another active project
10. Guild stored in `pendingBotGuilds[projectId]` → popup closes → auto-connect picks it up

## Changes

### Files to modify

| File | Change |
|---|---|
| `DiscordOAuthClient.java` | Add `exchangeBotCode()` returning `BotTokenResponse` with guild object |
| `DiscordAuthService.java` | Add nonce system (`BotNonce` record, `createBotNonce`, `consumeBotNonce`, TTL cleanup); add `createBotAuthUrl()` |
| `DiscordChannelService.java` | Update `getBotInviteUrl()` to accept `userId`, use nonce-based state |
| `DiscordChannelController.java` | Pass `@AuthenticationPrincipal` to `getBotInviteUrl()` |
| `DiscordAuthController.java` | Rewrite callback: parse state prefix, handle `bot:` flow (nonce lookup, token exchange, ownership check, guild bind), handle `user:` flow (existing), reject unknown state; handle `error` param for bot flow (close popup) |
| `LinkedChannelRepository.java` | Add `findByDiscordGuildIdAndIsActiveTrue()` |

### Not changed

- **Frontend** — no changes (invite URL endpoint returns same shape `{ url }`)
- **Bot** — no changes
- **Guild picker** — kept as fallback for edge cases

## Security

- **Self-describing state**: `bot:<nonce>` / `user:<nonce>` prefixes prevent ambiguity — no guessing by absence
- **Nonce stored server-side**: random UUID, mapped to `{projectId, userId, expiry}` — CSRF-safe, projectId not guessable from URL
- **Ownership check**: `guild.owner_id` from token response must match project owner's Discord ID
- **Already-bound check**: guild ID checked against DB before storing
- **Token exchange**: `client_secret` never exposed client-side; guild data comes from server-authenticated response, not user-facing URL
