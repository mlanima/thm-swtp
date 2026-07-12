# Frontend – Folder Structure & Architecture

Guide zur IdeaCamp-Frontend (Angular 21, Standalone, SSR). Auth via Keycloak/OIDC, i18n via `@ngx-translate`, Markdown-Rendering via `marked` + `dompurify`.

> Stand: Juli 2026 · Angular 21.2 · TypeScript 5.9 · `angular-oauth2-oidc` v20

---

## 📁 Project Structure

```
web/ideacamp/
├── src/
│   ├── main.ts                           # Bootstrap (browser)
│   ├── index.html
│   ├── styles.css                        # Global Styles (Tailwind)
│   └── app/
│       ├── app.ts                        # Root Component (Header/Sidebar/Footer/Toast)
│       ├── app.html · app.css
│       ├── app.config.ts                 # Browser Providers (Router, HTTP, OAuth, i18n)
│       ├── app.config.server.ts          # SSR Providers (merge mit app.config)
│       ├── app.routes.ts                 # Route Configuration (30 Routes)
│       ├── app.routes.server.ts          # SSR Render-Mode pro Route (Client/Prerender)
│       ├── app.spec.ts · i18n-keys.spec.ts
│       ├── enviroments/
│       │   └── enviroment.dev.ts         # apiUrl, issuer, clientId, googleMapsApiKey
│       ├── models/                       # Globale Datenmodelle (15 Files)
│       ├── services/                     # Globale Services (5)
│       ├── shared/                       # Wiederverwendbare Komponenten & Utils
│       └── feature/                      # Feature-Module (21+)
├── public/                               # Statische Assets
├── assets/i18n/                          # JSON-Locale-Files (de, …)
├── angular.json
├── proxy.conf.json                       # Dev-Proxy /api + /uploads → :8080
├── package.json · tsconfig.json · tsconfig.app.json · tsconfig.spec.json
├── eslint.config.js · .prettierrc · .postcssrc.json
├── Dockerfile
└── FRONTEND_README.md
```

### `src/app/models/` (globale Modelle)
`file-visibility.model.ts`, `github-connection.model.ts`, `github-readme.model.ts`, `github-repo-card.model.ts`, `link-visibility.model.ts`, `page-response.model.ts`, `project-file.model.ts`, `project-invite.model.ts`, `project-invite-member.model.ts`, `project-link.model.ts`, `project.model.ts`, `thesis.model.ts`, `user-ban-status.model.ts`, `user-profile-link.model.ts`, `user-profile.model.ts`

### `src/app/services/` (globale Services)
`language.service.ts` (i18n-Init), `project-favorite.service.ts`, `project-join-request.service.ts`, `user-follow.service.ts`, `user-profile.service.ts`

### `src/app/shared/`
```
shared/
├── header/            # Top-Nav, auth-panel, feature, logo
├── sidebar/           # Navigation, menu-link, recent, sidebar.service
├── footer/
├── toast/             # toast + toast.service
├── onboarding/
├── success-modal/
├── favorite-button/
├── follow-button/
├── join-request-button/
├── link-manager/      # + schema + types
├── place-autocomplete # Google Places
├── user-search-pick/
├── tags/tag-list/
├── edit-button/
├── icons/             # followers-icon, location-icon, …
├── pipes/             # markdown.pipe
├── utils/             # avatar, github-readme-renderer, relative-time
└── types/             # user.type
```

### `src/app/feature/` (Feature-Module)
```
feature/
├── auth/                    # AuthService, Guards, Interceptor, jwt-utils, success/
├── landing-page/            # Öffentliche Startseite
├── banned-account/          # Seite für gebannte User
├── dashboard/               # current-projects, current-theses, recent-posts
├── search/                  # search-page, project/user-result-card, search-filter-panel, search-input
├── project-create/         # Wizard: general/members/settings/finish-form + stepper + wizard-layout + schemas
├── project-site/           # Detailseite: header, sidebar, info-card, member-list, open-position-card, tag-list, github-repo-card, project-files, project-posts, project-readme + services
├── project-settings/       # Tabs: privacy, members, join-requests, github, discord, danger-zone + store + services + models
├── my-projects/            # project-card, project-list, project-filter, invitation-card, invitations-section + services
├── favorites/              # favorites-page
├── contact-request/        # contact-requests page + contact-request-box
├── thesis-create/         # Wizard: general/students/settings/finish-form + schemas
├── thesis-site/            # header, sidebar, info-card, student-list
├── thesis-settings/        # Tabs: students, danger-zone + services
├── my-theses/              # thesis-card, thesis-list, services
├── reports/                # User-Seite: report-dialog, schemas, service, models
├── professor-request/      # User-Antragsseite + services
├── github/                 # github-callback page + services
├── user-profile/          # user-profile page, profile-banner, profile-information, profile-tag-list + services
├── user-settings/         # Tabs: contact, discord, integrations, impressum, professor-request
├── moderator/             # mod-only: audit-logs, professor-request, projects, reports, user-management + services + shared/pagination
└── legal-notice/          # impressum
```

---

## 🔐 Authentication Architecture

### Overview

**Keycloak** als OAuth2/OIDC-IdP, integriert über `angular-oauth2-oidc`.

**Features:**
- ✅ Authorization Code Flow (response_type `code`)
- ✅ Automatisches Silent Refresh (`setupAutomaticSilentRefresh`)
- ✅ SSR-kompatibel (Auth-Bootstrap läuft erst nach `appRef.isStable`, Browser-only via `isPlatformBrowser`)
- ✅ Signal-basierter reaktiver State
- ✅ Rollen-Erkennung (`MODERATOR` realm role aus JWT `realm_access.roles`)
- ✅ Ban-Status-Check beim Route-Guard

### Auth Service (`feature/auth/auth.service.ts`)

**Zentrale Signale:**

| Signal | Typ | Bedeutung |
|--------|------|-----------|
| `isLoggedIn` | `WritableSignal<boolean>` | Gültiges Access-Token vorhanden |
| `isLoggingOut` | `WritableSignal<boolean>` | Logout läuft |
| `isModerator` | `WritableSignal<boolean>` | User hat realm role `MODERATOR` |
| `user` | `WritableSignal<User \| null>` | Minimal-User (`username`, `id`) |
| `username` | `WritableSignal<string>` | Convenience für UI |
| `currentBanStatus` | `WritableSignal<UserBanStatusModel \| null>` | Letzter Ban-Status |

**Key Methods:**

| Method | Zweck |
|--------|------|
| `login()` | Startet Code-Flow (`initCodeFlow`), mit Timeout-Fallback → direkter Redirect zum Keycloak-Auth-Endpoint (`redirectToKeycloakLogin`) |
| `logout()` | Setzt Signale zurück, `isLoggingOut=true`, `oauthService.logOut()` → Keycloak redirectet zu `postLogoutRedirectUri` (`/landing`) |
| `isAuthenticated()` | `hasValidAccessToken()` |
| `getAccessToken()` | JWT für API-Calls (oder `null`) |
| `waitUntilAuthReady()` | Async: wartet auf Discovery + `loadDiscoveryDocumentAndTryLogin` (idempotent über `initPromise`) |
| `loadCurrentBanStatus()` | HTTP `GET /v1/users/me/ban-status`, cached in `currentBanStatus` |

**Bootstrap-Reihenfolge:**
1. Konstruktor: `OAuthService` injecten, `AuthConfig` konfigurieren (`issuer`, `clientId`, `scope`, `redirectUri = origin/success`, `postLogoutRedirectUri = origin/landing`).
2. OAuth-Event-Subscription → `updateStateAfterTick`.
3. Warten auf `appRef.isStable` (SSR-sicher) → `startAuthBootstrap()`.
4. `loadDiscoveryDocumentAndTryLogin()` → `setupAutomaticSilentRefresh()` → `updateState()`.

**`User`-Typ** (`shared/types/user.type.ts`): Zod-Schema `{ username, id }`, exportiert via `z.infer`.

### JWT-Utils (`feature/auth/jwt-utils.ts`)
`decodeJwtPayload(token)`: base64url → base64 → `atob` → JSON. Genutzt von Interceptor (für `X-User-Id`) und `AuthService.hasModeratorRole`.

### Guards

Es gibt **drei Guards**. Alle sind SSR-sicher (frühes `return true` wenn `!isPlatformBrowser`).

#### `authGuard` (`feature/auth/auth.guard.ts`)
Schützt alle auth-pflichtigen Routes. Ablauf:
1. `waitUntilAuthReady()`
2. `isLoggingOut()` → redirect `/landing`
3. `isAuthenticated()`?
   - **Nein** → `state.url` in `sessionStorage.postLoginRedirectUrl`, `login()`, Route blockiert.
   - **Ja** → `loadCurrentBanStatus()` (mit Fehler-Fallback `banned:false`)
     - `banned` → redirect `/account-banned`
     - `isAuthCallbackRoute(url)` (== `/success`) → `true`
     - `isModerator()` → nur `/project/:url` und `/profiles/:username` sind für Moderatoren lesbar (`moderator-readable-routes.ts`), sonst redirect `/moderator`
     - sonst `true`

#### `moderatorGuard` (`feature/auth/moderator.guard.ts`)
Schützt `/moderator/**`. Auth → Ban-Check (`banned` → `/account-banned`) → `isModerator()` ? `true` : redirect `/landing`.

#### `bannedAccountGuard` (`feature/auth/banned-account.guard.ts`)
Schützt `/account-banned`. Lässt nur gebannte User zu; nicht-gebannte Moderatoren → `/moderator`, andere → `/landing`.

### Auth Interceptor (`feature/auth/auth.interceptor.ts`)
Klassischer `HttpInterceptor` (via `HTTP_INTERCEPTORS`-Token, `multi: true`).
- Token aus `auth.getAccessToken()`.
- Nur Requests, deren URL mit `environment.apiUrl` beginnt, werden angereichert.
- Header: `Authorization: Bearer {token}` und `X-User-Id: {sub}` (aus `decodeJwtPayload`).
- Sonst: Request unverändert weiterreichen.

### OAuth Callback (`feature/auth/success/success.component.ts`)
Keycloak redirectet nach `{origin}/success?code=…&state=…`. Die `SuccessComponent` mountet; `AuthService` verarbeitet den Callback (`loadDiscoveryDocumentAndTryLogin` tauscht den Code). Guard lässt die Callback-Route passieren (`isAuthCallbackRoute`), danach Redirect zum gespeicherten `postLoginRedirectUrl` oder Dashboard.

---

## 🎯 Feature Modules (Detail)

| Modul | Zweck |
|------|--------|
| **auth** | OAuth2/OIDC, Guards, Interceptor, JWT-Utils, Success-Callback |
| **landing-page** | Öffentliche Startseite (kein Guard) |
| **legal-notice** | Impressum (öffentlich) |
| **banned-account** | Seite für gebannte User (`bannedAccountGuard`) |
| **user-profile** | Profil-Anzeige/Bearbeitung (`profile-banner`, `profile-information`, `profile-tag-list`) |
| **user-settings** | Settings mit Tabs: contact, discord, integrations, impressum, professor-request |
| **dashboard** | Übersicht: current-projects, current-theses, recent-posts |
| **search** | Projekt-/Usersuche, Filter-Panel, Result-Cards, Input |
| **project-create** | Wizard (general/members/settings/finish) + stepper + wizard-layout + Zod-schemas |
| **project-site** | Projektdetail: header, sidebar, info-card, member-list, open-position-card, tag-list, github-repo-card, project-files, project-posts, project-readme |
| **project-settings** | Settings-Tabs: privacy, members, join-requests, github, discord, danger-zone + `project-settings.store.ts` |
| **my-projects** | Projekt-Dashboard: cards, list, filter, invitations-section + invitation-card |
| **favorites** | Favoriten-Seite |
| **contact-request** | Einladungen: contact-requests page + contact-request-box |
| **thesis-create** | Wizard (general/students/settings/finish) + schemas |
| **thesis-site** | Thesen-Detail: header, sidebar, info-card, student-list |
| **thesis-settings** | Tabs: students, danger-zone + services |
| **my-theses** | Thesen-Dashboard: thesis-card, thesis-list |
| **reports** | User-Meldungen: report-dialog, schemas, service |
| **professor-request** | User-Antrag auf Professor-Rechte + services |
| **github** | GitHub OAuth-Callback + services |
| **moderator** | Nur `MODERATOR`: audit-logs, professor-request, projects (+delete-dialog, project-table), reports (+report-table, -detail-dialog, -action-menu), user-management (+ban-user-dialog), shared/pagination, services |

---

## 📦 Shared Components & Services

### Shared Components

| Komponente | Ort | Zweck |
|-----------|------|-------|
| Header | `shared/header/` | Top-Nav mit `auth-panel`, `feature`, `logo` |
| Sidebar | `shared/sidebar/` | Navigation, `menu-link`, `recent` |
| Footer | `shared/footer/` | Footer |
| Toast | `shared/toast/` | Toast-Notifications (+ `toast.service`) |
| Onboarding | `shared/onboarding/` | Onboarding-Overlay |
| SuccessModal | `shared/success-modal/` | Bestätigungs-Modal |
| FavoriteButton | `shared/favorite-button/` | Favorit toggeln |
| FollowButton | `shared/follow-button/` | Follow toggeln |
| JoinRequestButton | `shared/join-request-button/` | Join-Request senden |
| LinkManager | `shared/link-manager/` | Link-Liste-Editor (+ schema, types) |
| PlaceAutocomplete | `shared/place-autocomplete/` | Google Places Location-Autocomplete |
| UserSearchPick | `shared/user-search-pick/` | User-Auswahl-Picker |
| TagList | `shared/tags/tag-list/` | Tag-Anzeige |
| EditButton | `shared/edit-button/` | Bearbeiten-Button |
| Icons | `shared/icons/` | SVG-Icons (`followers-icon`, `location-icon`, …) |

### Pipes & Utils

| Datei | Zweck |
|------|-------|
| `pipes/markdown.pipe.ts` | Markdown → sanitisiertes HTML (via `marked` + `dompurify`) |
| `utils/avatar.util.ts` | Avatar-Helfer |
| `utils/github-readme-renderer.ts` | README-Markdown-Renderer |
| `utils/relative-time.util.ts` | Relative Zeit-Ausgabe |

### Shared Services
- `SidebarService` (`shared/sidebar/sidebar.service.ts`) – Sidebar-State
- `ToastService` (`shared/toast/toast.service.ts`)
- `LanguageService` (`services/language.service.ts`) – i18n-Init

### Shared Types
- `User` (`shared/types/user.type.ts`) – Zod-Schema `{ username, id }`

---

## 🚀 Environment Configuration

**File:** `src/app/enviroments/enviroment.dev.ts`

```typescript
export const environment = {
  apiUrl: 'http://localhost:8080/api',
  keycloakUrl: 'https://auth.swtp-ss26.de',
  issuer: 'https://auth.swtp-ss26.de/realms/swtp',
  clientId: 'swtp-frontend',
  scope: 'openid profile email',
  googleMapsApiKey: '',
};
```

> Hinweis: Ordner heißt absichtlich `enviroments` (historischer Tippfehler, beibehalten).

**Dev-Proxy** (`proxy.conf.json`): leitet `/api` und `/uploads` an `http://localhost:8080` weiter (`changeOrigin: true`).

---

## 📦 Technology Stack

**Framework & Libs:**
- Angular 21.2 (Standalone Components, Signals)
- TypeScript 5.9 (strict)
- Angular Router (Standalone Routes)
- RxJS 7.8
- `angular-oauth2-oidc` 20.0.2 (Keycloak/OIDC)
- `@ngx-translate/core` 18 + `@ngx-translate/http-loader` (i18n, `assets/i18n/*.json`, fallback `de`)
- `zod` 4.4 (Form-/Schema-Validation)
- `marked` 18 + `marked-gfm-heading-id` + `dompurify` 3.4 (Markdown-Rendering)
- `primeicons` 7
- `express` 5 (SSR-Server)

**Styling:**
- Tailwind CSS 4.1 (`@tailwindcss/postcss`)
- PostCSS

**Build & Dev:**
- Angular CLI 21.2 / `@angular/build` 21.2 (esbuild/vite-basiert)
- Angular SSR (`@angular/ssr` + Express, `serve:ssr:ideacamp`)

**Testing:**
- Vitest 4.0 + JSDOM 28
- Angular Testing Utilities
- `*.spec.ts` kolokiert mit Sources

**Code Quality:**
- ESLint (`angular-eslint` 21.4 + `typescript-eslint` 8.59)
- Prettier 3.8

---

## 🔄 Data Flow

### Login Flow
```
1. Guard ruft auth.login() (oder User klickt Login)
2. AuthService.startAuthBootstrap() → initCodeFlow()
3. Redirect zu Keycloak /protocol/openid-connect/auth (Fallback nach 1,5s)
4. Keycloak: User authentifiziert → redirect {origin}/success?code=…&state=…
5. SuccessComponent mountet, loadDiscoveryDocumentAndTryLogin() tauscht Code
6. updateState() setzt isLoggedIn/isModerator/user/username
7. Redirect zu sessionStorage.postLoginRedirectUrl (oder Default)
```

### Protected Route Access
```
1. Navigation zu z.B. /dashboard
2. authGuard: waitUntilAuthReady()
3. Nicht authentifiziert → login(), Route blockiert
4. Authentifiziert → loadCurrentBanStatus()
   - banned  → /account-banned
   - isModerator() && route nicht lesbar → /moderator
   - sonst → Komponente laden
```

### API Call with Token
```
1. Komponente ruft HttpClient (URL beginnt mit environment.apiUrl)
2. AuthInterceptor: token = auth.getAccessToken()
3. decodeJwtPayload(token) → sub
4. Request cloned mit Authorization: Bearer {token}, X-User-Id: {sub}
5. Backend validiert JWT
```

### SSR Render-Modi (`app.routes.server.ts`)
- Auth-geschützte & dynamische Routes → `RenderMode.Client` (`/search`, `/dashboard`, `/project/:projectUrl`, `/moderator/**`, …)
- `**` (Rest, inkl. Landing/Impressum) → `RenderMode.Prerender`
- Server-Config wird in `app.config.server.ts` via `mergeApplicationConfig` mit `appConfig` kombiniert.

---

## 📝 Routing Summary

| Path | Component | Guard | Zweck |
|------|-----------|-------|-------|
| `/` | – | – | Redirect → `/landing` |
| `/landing` | LandingPage | – | Öffentliche Startseite |
| `/impressum` | Impressum | – | Impressum |
| `/success` | SuccessComponent | `authGuard` | OAuth-Callback |
| `/github/callback` | GithubCallback | `authGuard` | GitHub OAuth-Callback |
| `/account-banned` | BannedAccount | `bannedAccountGuard` | Gebannte User |
| `/profiles/:username` | UserProfile | `authGuard` | Profilseite |
| `/settings` | UserSettings | `authGuard` | User-Settings (Tabs) |
| `/professor-request` | – | – | Redirect → `/settings` |
| `/dashboard` | DashboardPage | `authGuard` | Übersicht |
| `/search` | SearchPage | `authGuard` | Suche |
| `/createProject` | ProjectCreate | `authGuard` | Projekt-Wizard |
| `/createThesis` | ThesisCreate | `authGuard` | Thesen-Wizard |
| `/my-projects` | MyProjectsPage | `authGuard` | Eigene Projekte |
| `/my-theses` | MyThesesPage | `authGuard` | Eigene Thesen |
| `/favorites` | FavoritesPage | `authGuard` | Favoriten |
| `/contact-requests` | ContactRequests | `authGuard` | Einladungen |
| `/project/:projectUrl` | ProjectSite | `authGuard` | Projekt-Detail |
| `/project/:projectUrl/settings` | ProjectSettings | `authGuard` | Projekt-Settings |
| `/thesis/:thesisUrl` | ThesisSite | `authGuard` | Thesen-Detail |
| `/thesis/:thesisUrl/settings` | ThesisSettings | `authGuard` | Thesen-Settings |
| `/moderator` | ModeratorPage | `moderatorGuard` | Moderator-Landing |
| `/moderator/projects` | ProjectsComponent | `moderatorGuard` | Projekt-Verwaltung |
| `/moderator/users` | UserManagement | `moderatorGuard` | User-Management (Ban) |
| `/moderator/professor-requests` | ProfessorRequestComponent | `moderatorGuard` | Anträge verwalten |
| `/moderator/audit-logs` | AuditLogsComponent | `moderatorGuard` | Audit-Logs |
| `/moderator/reports` | ReportManagement | `moderatorGuard` | Meldungs-Verwaltung |

> Öffentlich (kein Guard): `/landing`, `/impressum`, `/` (Redirect). `/success` und `/github/callback` tragen `authGuard`, der die Callback-Route passieren lässt.

---

## 🧪 Testing

```bash
npm test      # ng test (Vitest + JSDOM)
npm run lint  # ng lint (ESLint)
```

Test-Files: `*.spec.ts`, kolokiert mit Sources (z. B. `auth.service.spec.ts`, `auth.guard.spec.ts`, `sidebar.service.spec.ts`, `github-readme-renderer.spec.ts`, `i18n-keys.spec.ts`, `app.spec.ts`).

---

## 📚 Key Design Patterns

1. **Standalone Components** – keine NgModules.
2. **Functional Routing** – Routes als Config-Objekte, drei `CanActivateFn`-Guards.
3. **Angular Signals** – reaktiver State statt `BehaviorSubject` (AuthService, Store).
4. **SSR mit Route-Modi** – `RenderMode.Client`/`Prerender` pro Route, Bootstrap erst nach `appRef.isStable`.
5. **HTTP Interceptor (klassisch)** – via `HTTP_INTERCEPTORS`-Token (`withInterceptorsFromDi`).
6. **Feature-basierte Struktur** – `feature/` nach Geschäftsfeature, `shared/` für Wiederverwendung.
7. **Zod-Validation** – Forms + `User`-Typ.
8. **i18n** – `@ngx-translate`, Keys in `assets/i18n/`, `TranslateService.instant()` statt roher Keys im Template (siehe `web/ideacamp/CLAUDE.md`).
9. **Markdown** – `MarkdownPipe` + `github-readme-renderer` mit `dompurify`-Sanitizing.

---

## 🔗 Related Documentation

- [Backend API Documentation](./BACKEND_API_DOCUMENTATION.md)
- [Keycloak Integration Frontend & Backend](./Keycloak%20-%20Integration%20Frontend%20%26%20Backend.md)
- [Keycloak Setup & Konfiguration](./Keycloak%20-%20Setup%20%26%20Konfiguration.md)
- [Frontend Auth Flow](./frontend-auth-flow.md)
- Frontend-Conventions: `web/ideacamp/CLAUDE.md`, `web/ideacamp/FRONTEND_README.md`

---

*Struktur und Guards aus `app.routes.ts`, `app.routes.server.ts`, `app.config.ts`, Guard-Quellen, `auth.service.ts` und der tatsächlichen Verzeichnisstruktur extrahiert. Bei Codeänderungen bitte nachpflegen.*