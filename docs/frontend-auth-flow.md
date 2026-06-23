# Frontend Auth Flow

> **Last updated:** 2026-06-23

This document describes the authentication and role-based flow in the Angular frontend.

## Auth Service (`auth.service.ts`)

The `AuthService` is the central service for OIDC authentication via Keycloak.

### Reactive State Signals

| Signal | Type | Description |
|--------|------|-------------|
| `isLoggedIn` | `WritableSignal<boolean>` | True when a valid access token is present |
| `isLoggingOut` | `WritableSignal<boolean>` | True during the logout process |
| `isModerator` | `WritableSignal<boolean>` | True when the user has the Keycloak realm role `MODERATOR` |
| `user` | `WritableSignal<User \| null>` | Minimal authenticated user (`username`, `id`) |
| `username` | `WritableSignal<string>` | Convenience signal for the current username |

### Role Detection

The `isModerator` signal is updated in `updateState()` by decoding the JWT access token extracted
via `getAccessToken()`. The token's base64url payload is parsed for
`realm_access.roles`, and `isModerator` is set to `true` if the array includes `"MODERATOR"`.

A shared `decodeJwtPayload` helper in `jwt-utils.ts` handles the base64url-to-base64
conversion (`-` → `+`, `_` → `/`, correct padding) and is used by both the auth service
and the HTTP interceptor.

```typescript
export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}
```

## Guards

### `authGuard` (`auth.guard.ts`)

Used by routes intended for regular (non-moderator) users.

- Waits for auth to be ready (discovery document loaded, token restored).
- Redirects to `/impressum` if the user is currently logging out.
- Redirects to the Keycloak login if not authenticated.
- Allows access for authenticated regular users.
   - **Redirects moderators** to `/moderator` — moderators may not access regular user pages.

### `moderatorGuard` (`moderator.guard.ts`)

Used by the `/moderator` route to restrict access to moderators only.

- Waits for auth to be ready.
- Redirects to Keycloak login if not authenticated.
- Redirects to `/landing` if authenticated but not a moderator (`isModerator()` is `false`).
- Allows access only if `isModerator()` is `true`.

## Post-Login Flow (`success.component.ts`)

The `/success` route is the OIDC redirect target after login. The component:

1. Waits for auth to be ready.
2. Redirects to `/impressum` if authentication failed.
3. **If the user is a moderator** (`isModerator()` is `true`):
   - Redirects to `/moderator` immediately.
   - Does **not** call `getMyProfile()`, so no `UserProfile` is created for moderators.
4. **If the user is a regular user**:
   - Calls `getMyProfile()` to create or synchronize the user profile.
   - The user stays on the success page (the UI shows a welcome message).

## Routes

| Path | Component | Guard | Access |
|------|-----------|-------|--------|
| `/landing` | `LandingPage` | None | Public |
| `/success` | `SuccessComponent` | None | Public (OIDC redirect) |
| `/moderator` | `ModeratorPage` | `moderatorGuard` | Moderators only |
| `/profiles/:username` | `UserProfile` | `authGuard` | Regular users only (moderators redirected to `/moderator`) |
| `/my-projects` | `MyProjectsPage` | `authGuard` | Regular users only |
| `/search` | `SearchPage` | `authGuard` | Regular users only |
| … | … | … | … |

## Sidebar

The sidebar shows different content based on role:

- **Moderators:** Only the "Moderator" link (shield icon) and the logout button are
  shown. Regular user links (search, my-projects, favorites, profile, settings)
  are hidden.
- **Regular users:** All standard navigation links are shown. The invitation
  badge is loaded for my-projects.

## Header

The header adapts based on role:

- **Moderators:** The nav bar (home/explore links) and profile link are hidden.
  A minimal panel shows the username and a logout button.
- **Regular users:** The nav bar and profile link (linking to `/profiles/:username`)
  are displayed as before.
