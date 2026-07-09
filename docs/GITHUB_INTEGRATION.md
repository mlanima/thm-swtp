# GitHub Integration

## Overview

IdeaCamp integrates with GitHub in two layers:

1. **Per-user GitHub connection** — every user can link their GitHub account via
   OAuth (authorization-code flow). The access token is stored encrypted and used
   for all GitHub API calls made on that user's behalf.
2. **Per-project repository link** — a project owner/editor can link exactly one
   GitHub repository to a project. The linked repo powers:
   - a **repo card** on the project page (stars, forks, description, top languages),
   - an optional **README section** rendered on the project page,
   - optional **auto-invite**: new project members with a connected GitHub account
     are automatically invited as collaborators on the repo.

Unrelated to this feature: `GitHubTopicsClient` (under `api/.../tag/validation/`)
also calls the GitHub API, but only to validate tags against GitHub topics — see
`TAG_VALIDATION.md`.

---

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                      Angular Frontend                         │
│                                                               │
│  User Settings ▸ Integrations   Project Settings ▸ GitHub     │
│  (connect / disconnect)         (link repo, toggles)          │
│                                                               │
│  /github/callback page          Project page:                 │
│  (completes OAuth)              repo card + README renderer   │
└───────────────────────┬───────────────────────────────────────┘
                        │ HTTP (REST, Keycloak JWT)
                        ▼
┌───────────────────────────────────────────────────────────────┐
│                      Spring Boot API                          │
│                                                               │
│  github/                        projectGithubRepo/            │
│  ├─ GithubConnectionController  ├─ ProjectGithubRepoController│
│  ├─ GithubConnectionService     ├─ ProjectGithubRepoService   │
│  ├─ GithubStateService (HMAC)   ├─ GithubRepoDataService      │
│  ├─ TokenCipher (AES-256-GCM)   │   (cached via Redis)        │
│  ├─ GithubOAuthClient ──────────┼─ GithubCollaboratorInvite-  │
│  └─ GithubApiClient             │   Listener (async, event)   │
└──────────┬──────────────────────┴──────────┬──────────────────┘
           │                                 │
           ▼                                 ▼
  github.com (OAuth:               api.github.com (REST:
  authorize, token exchange,       /user, /repos, /languages,
  grant revocation)                /readme, /collaborators)
```

Backend packages:

- `api/src/main/java/de/thm/swtp/api/github/` — the per-user OAuth connection
  (controller, service, state signing, token encryption, GitHub HTTP clients,
  exceptions).
- `api/src/main/java/de/thm/swtp/api/projectGithubRepo/` — the project↔repo link
  (controller, services, entity, collaborator-invite listener).

Frontend:

- `web/ideacamp/src/app/feature/github/` — connection service + OAuth callback page.
- `web/ideacamp/src/app/feature/user-settings/tabs/integrations-tab/` — connect/disconnect UI.
- `web/ideacamp/src/app/feature/project-settings/tabs/github-tab/` — repo linking UI.
- `web/ideacamp/src/app/feature/project-site/components/github-repo-card/` and
  `project-readme/` — display on the project page.
- `web/ideacamp/src/app/shared/utils/github-readme-renderer.ts` — README markdown → safe HTML.

---

## User connection (OAuth flow)

The classic GitHub OAuth **authorization-code** flow, with the callback handled by
the SPA (the backend never receives a browser redirect):

1. **Authorize URL** — the frontend calls `POST /api/v1/github/connection/authorize-url`.
   `GithubConnectionService.buildAuthorizeUrl()` builds
   `https://github.com/login/oauth/authorize?client_id=…&redirect_uri=…&scope=repo&state=…`.
2. **State parameter** — `GithubStateService` mints the `state` **statelessly**: the
   payload `userId:expiresAt:nonce` is HMAC-SHA256-signed with a key derived from
   the token-encryption secret (`SHA-256("github-state:" + key)`). No server-side
   session storage is needed. TTL is 10 minutes; validation checks signature,
   expiry, and that the state's user id matches the authenticated caller.
3. **Redirect to GitHub** — the frontend stores a `github_return_to` path in
   `sessionStorage` and navigates to the authorize URL.
4. **Callback** — GitHub redirects to `<app>/github/callback?code=…&state=…`. The
   `GithubCallback` page (auth-guarded route) posts `code` + `state` to
   `POST /api/v1/github/connection/callback`.
5. **Code exchange** — `GithubOAuthClient.exchangeCode()` exchanges the code at
   `https://github.com/login/oauth/access_token`, then `GithubApiClient
   .getAuthenticatedUser()` fetches the GitHub user (id, login, avatar).
6. **Persist** — the connection is upserted into `github_connections` (keyed by
   the Keycloak user id, one connection per user) with the token encrypted at rest.
   As a one-time convenience, a "GitHub" link to the user's profile page is added
   to their user-profile links (never re-synced afterwards).
7. **Redirect back** — the callback page navigates to the stored `github_return_to`
   path (default `/settings`).

**Disconnect** (`DELETE /api/v1/github/connection`) deletes the row and revokes the
OAuth grant at GitHub (best-effort — revocation failure is logged and ignored).

### Token storage & connection status

- Tokens are encrypted with **AES-256-GCM** (`TokenCipher`), keyed by
  `github.oauth.token-encryption-key` (Base64, 32 bytes). Stored format:
  `Base64(12-byte random IV || ciphertext+tag)`.
- `GithubConnectionStatus` is `ACTIVE` or `INVALID`. Whenever any GitHub call
  returns 401, the connection is marked `INVALID` (`markInvalid`) — the user must
  reconnect. Other features obtain tokens only via
  `GithubConnectionService.getActiveDecryptedToken(userId)`, which returns empty
  for missing or `INVALID` connections.

---

## Project repository link

Stored in `project_github_repos` (`ProjectGithubRepoEntity`): one repo per project
(`project_id` unique), with `repoOwner`, `repoName`, `defaultBranch`,
`linkedByKeycloakId` (who linked it), and the flags `showReadme` and
`autoInviteCollaborators` (both default `false`).

### Linking

`PUT /api/v1/projects/{projectId}/github-repo` with `{ repoOwner, repoName }`:

- Requires the caller's own **active** GitHub connection
  (else 409 `GithubConnectionRequiredException`).
- The repo is fetched with the caller's token; the caller must have **write access**
  (`permissions.push` or `admin` in the API response — absent on unauthenticated
  responses, which counts as no access; else 403 `GithubRepoAccessDeniedException`).
- Re-linking replaces the existing link (upsert). The linker's user id is stored and
  their token is used for all later data fetches for this repo.

The frontend accepts either `owner/repo` or a full GitHub URL and parses it client-side.

### Repo card & README data

`GithubRepoDataService` fetches the display data (repo metadata + top 6 languages
with percentage shares, and the raw README markdown):

- **Token fallback**: it tries the linker's active token first (higher rate limit,
  private-repo access). On 401 the connection is marked invalid and it retries
  unauthenticated. On 404/API errors it returns `null` — the card renders a
  `dataUnavailable` state instead of failing the request.
- **Caching**: results are cached in Redis (`CacheConfig`) — `github-repo-card`
  for 10 minutes, `github-readme` for 30 minutes. Cache keys are `owner/repo`
  only; tokens never enter the cache. Without Redis, caching is a no-op.

### README rendering (frontend)

`GET …/github-repo/readme` returns raw markdown (only if `showReadme` is enabled,
else 404). `renderGithubReadme()` turns it into safe HTML:

1. `marked` (with GFM heading ids) converts markdown to HTML.
2. `DOMPurify` sanitizes — this is **the** trust boundary for repo-owner-controlled
   content (Angular's own `[innerHTML]` sanitizer is bypassed on top of it).
3. Relative image/link paths are rewritten to absolute
   `raw.githubusercontent.com` / `github.com` URLs against the default branch.
4. Bare `#fragment` links are rewritten against the current path so in-page
   ToC navigation works despite the app's `<base href="/">`.

### Auto-invite collaborators

`GithubCollaboratorInviteListener` listens (async, `AFTER_COMMIT`) for
`ProjectMemberAddedEvent`. If the project has a linked repo with
`autoInviteCollaborators` enabled **and** the new member has a GitHub connection
**and** the linker still has an active token, the new member is invited with
`push` permission via `PUT /repos/{owner}/{repo}/collaborators/{username}`.
Strictly best-effort: any failure is only logged and never affects the membership
grant itself. A rejected linker token marks the linker's connection invalid.

---

## REST API

All endpoints require a Keycloak JWT; method security via `SecurityService` (`@security.…`).

### Connection (`/api/v1/github/connection`)

| Method | Path             | Auth rule                    | Description |
|--------|------------------|------------------------------|-------------|
| GET    | ``               | `canManageGithubConnection` (regular user) | Connection status (`enabled`, `connected`, `githubLogin`, `avatarUrl`, `status`) |
| POST   | `/authorize-url` | same                         | Returns the GitHub authorize URL incl. signed state |
| POST   | `/callback`      | same                         | Body `{ code, state }`; completes the connection |
| DELETE | ``               | same                         | Disconnect + best-effort grant revocation (204) |

### Project repo (`/api/v1/projects/{projectId}/github-repo`)

| Method | Path                 | Auth rule                          | Description |
|--------|----------------------|------------------------------------|-------------|
| PUT    | ``                   | `canManageProjectGithubRepo` (project editor) | Link/replace repo; body `{ repoOwner, repoName }` |
| GET    | ``                   | `canViewProjectGithubRepo` (project viewer)   | Repo card (link + cached data, `dataUnavailable` flag) |
| DELETE | ``                   | manage                             | Unlink (204) |
| GET    | `/readme`            | view                               | Raw README markdown (404 if not enabled/linked) |
| PATCH  | `/readme-visibility` | manage                             | Body `{ show }` |
| PATCH  | `/auto-invite`       | manage                             | Body `{ enabled }` |

### Error mapping (`GlobalExceptionHandler`)

| Exception                          | Status | Meaning |
|------------------------------------|--------|---------|
| `GithubIntegrationDisabledException` | 503  | OAuth env vars not configured |
| `InvalidGithubStateException`      | 400    | Bad/expired/foreign `state` |
| `GithubOAuthException`             | 400    | Code exchange failed |
| `GithubTokenInvalidException`      | 409    | Stored token rejected by GitHub — reconnect |
| `GithubConnectionRequiredException`| 409    | Action needs a connected GitHub account |
| `GithubApiException`               | 502    | GitHub API failure |
| `GithubRepoNotFoundException`      | 404    | Repo (or its README) not found |
| `GithubRepoNotLinkedException`     | 404    | Project has no linked repo |
| `GithubRepoAccessDeniedException`  | 403    | Linker lacks write access to the repo |
| `GithubReadmeNotEnabledException`  | 404    | README display not enabled |

The frontend's GitHub tab maps 409 → "connect GitHub first", 403 → "no write
access", 404 → "repo not found".

---

## Configuration

`application.yaml` (`github.oauth.*`, bound to `GithubOAuthProperties`):

| Property / env var | Default | Purpose |
|--------------------|---------|---------|
| `GITHUB_OAUTH_CLIENT_ID` | — | GitHub OAuth app client id |
| `GITHUB_OAUTH_CLIENT_SECRET` | — | OAuth app client secret |
| `GITHUB_OAUTH_REDIRECT_URI` | `http://localhost:4200/github/callback` | Must match the OAuth app callback and the SPA route |
| `GITHUB_TOKEN_ENCRYPTION_KEY` | — | Base64-encoded 32-byte AES key (also derives the state-signing key) |
| `github.oauth.scopes` | `repo` | Requested OAuth scope |
| `github.api.base-url` | `https://api.github.com` | Overridable for tests |

**Feature toggle**: there is no explicit flag — the integration counts as *enabled*
iff client id, client secret, and encryption key are all set
(`GithubOAuthProperties.enabled()`). When disabled, mutating endpoints return 503
and the UI hides the connect option (the status response carries `enabled`).

Generate a key with: `openssl rand -base64 32`.

### Deployment

The env vars are wired in `infra/swtp-ss26.de/stacks/swtp-main/` and `swtp-dev/`
(see the `.env.example` files); each stack sets its own
`GITHUB_OAUTH_REDIRECT_URI` (e.g. `https://dev.swtp-ss26.de/github/callback`).
A single GitHub OAuth app covers all `swtp-ss26.de` environments, including PR
review apps, because GitHub matches callback-URL subdomains at any depth.

---

## Security notes

- **CSRF on the OAuth flow**: the signed, expiring, user-bound `state` parameter
  prevents cross-user code injection; validation compares HMACs constant-time
  (`MessageDigest.isEqual`).
- **Tokens at rest**: AES-256-GCM with a random IV per encryption; tokens are never
  logged, never cached, and only leave `GithubConnectionService` as decrypted
  strings via `getActiveDecryptedToken`.
- **README content**: repo-owner-controlled markdown is sanitized with DOMPurify
  before display; that is the single trust boundary.
- **Write-access check on link**: prevents attaching arbitrary third-party repos
  to a project (the linker must have push/admin rights).
- **Auto-invite consent**: users are only invited if they themselves connected a
  GitHub account; the invite uses the *linker's* token, and GitHub still asks the
  invitee to accept (for personal repos).
