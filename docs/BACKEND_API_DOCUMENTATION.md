# API Backend – Dokumentation

> Stand: Juli 2026 · Spring Boot 4.0.6 · Java 25
> Alle Endpunkte liegen unter `/api` bzw. `/api/v1`. Alle nicht explizit als öffentlich markierten Pfade erfordern ein gültiges JWT (siehe [Security](#-security--authentication)).

## 📁 Projektstruktur

```
api/
├── src/
│   ├── main/java/de/thm/swtp/api/
│   │   ├── ApiApplication.java
│   │   ├── config/                    # Security, CORS, Cache, JWT-Converter, BannedUserFilter
│   │   ├── controller/               # HelloController
│   │   ├── common/                    # PageResponse u. a. Shared Utilities
│   │   ├── exceptionhandling/         # GlobalExceptionHandler + zentrale Exceptions
│   │   ├── auditlog/                  # Audit-Log Modul
│   │   ├── discord/                   # Discord Integration (OAuth, Channels, Settings, Status)
│   │   ├── github/                    # GitHub Integration (Connection, Repo-Link, README)
│   │   ├── links/                     # Project- & UserProfile-Links
│   │   ├── location/                  # Google Places Hilfscode
│   │   ├── moderation/                # Content-Moderation
│   │   ├── notification/              # Event-basierte Notifications
│   │   ├── professorRequest/          # Anträge auf Professor-Rechte
│   │   ├── project/                    # Project CRUD & Management
│   │   ├── projectFavorite/           # Favoriten (Likes)
│   │   ├── projectFiles/              # Datei-Uploads pro Projekt
│   │   ├── projectGithubRepo/        # GitHub-Repo-Verknüpfung pro Projekt
│   │   ├── projectInvitation/        # Projekt-Einladungen (direkt)
│   │   ├── projectJoinRequest/       # Öffentliche Beitrittsgesuche
│   │   ├── projectPost/               # Posts/Ankündigungen pro Projekt
│   │   ├── projectView/               # View-Counter
│   │   ├── reports/                   # Meldungen (Moderation)
│   │   ├── search/                     # Suche (Projects & Users)
│   │   ├── tag/                        # Tags für Projekte, Profile, globales Tag-Verzeichnis
│   │   ├── thesis/                     # Abschlussarbeiten + Zuordnungs-Notifications
│   │   ├── userFollow/                 # Follow / Follower
│   │   └── userprofile/               # Profile, Onboarding, User-Management (Ban)
│   ├── test/java/de/thm/swtp/api/
│   └── resources/
│       ├── application.yaml            # SQLite (dev) + Redis + OAuth2
│       ├── application-mysql.yaml     # MySQL/MariaDB Profil (prod)
│       ├── bad-words/ · i18n/ · templates/
├── pom.xml
├── Dockerfile
└── db/
    └── dev.db (SQLite)
```

---

## 🔌 REST API Endpoints

Legende: ✅ JWT erforderlich · ❌ öffentlich (permitAll) · 🔒 zusätzlich method-level (`@PreAuthorize("@security.…")`)

### 1. Authentication & Hello
**Base Path:** `/api`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/public/hello` | ❌ | Public hello endpoint |
| `GET` | `/api/hello` | ✅ | Secured hello – returns username, userId, roles |

---

### 2. Projects
**Base Path:** `/api/v1/projects`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects` | ✅ 🔒 | Alle Projekte (paginiert, Filter `name`) |
| `POST` | `/api/v1/projects` | ✅ 🔒 | Projekt anlegen |
| `GET` | `/api/v1/projects/{projectId}` | ✅ 🔒 | Projekt nach ID |
| `GET` | `/api/v1/projects/by-url/{projectUrl}` | ✅ 🔒 | Projekt nach URL |
| `PUT` | `/api/v1/projects/{projectId}` | ✅ 🔒 | Projekt aktualisieren (Owner) |
| `DELETE` | `/api/v1/projects/{projectId}` | ✅ 🔒 | Projekt löschen (Owner) |
| `PATCH` | `/api/v1/projects/{projectId}/allow-join-requests?allow=` | ✅ 🔒 | Join-Requests erlauben/verbieten (Owner) |
| `GET` | `/api/v1/projects/{projectId}/members` | ✅ 🔒 | Mitglieder eines Projekts |
| `DELETE` | `/api/v1/projects/{projectId}/members/{memberId}` | ✅ 🔒 | Mitglied entfernen (Owner) |
| `PATCH` | `/api/v1/projects/{projectId}/owner` | ✅ 🔒 | Ownership übertragen (Owner) |
| `GET` | `/api/v1/projects/url-exists/{projectUrl}` | ✅ | Prüft, ob Projekt-URL bereits vergeben ist |

**DTOs:**
- Request: `CreateProjectRequest` (`name`, `description`, `shortDescription`, `projectUrl`, `isPrivateProject`, `memberIds`, `tagIds`), `UpdateProjectRequest`, `TransferProjectOwnershipRequest` (`newOwnerId`)
- Response: `ProjectResponse` (inkl. `stats: ProjectStatsResponse`, `favoriteCount`), `ProjectMemberResponse`, `DeleteProjectResponse`

---

### 3. Project Files
**Base Path:** `/api/v1/projects/{projectId}/files`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/files` | ✅ 🔒 | Dateien auflisten |
| `POST` | `/api/v1/projects/{projectId}/files` | ✅ 🔒 | Datei hochladen (`multipart/form-data`, `visibility`) |
| `GET` | `/api/v1/projects/{projectId}/files/{fileId}/download` | ✅ 🔒 | Datei herunterladen |
| `PATCH` | `/api/v1/projects/{projectId}/files/{fileId}` | ✅ 🔒 | Sichtbarkeit ändern (Owner) |
| `DELETE` | `/api/v1/projects/{projectId}/files/{fileId}` | ✅ 🔒 | Datei löschen (Owner) |

**DTOs:** `ProjectFileResponse`, `UpdateProjectFileRequest` (`visibility: FileVisibility`)

---

### 3. Project Posts
**Base Path:** `/api/v1/projects/{projectId}/posts`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/posts` | ✅ 🔒 | Veröffentlichte Posts |
| `GET` | `/api/v1/projects/{projectId}/posts/drafts` | ✅ 🔒 | Entwürfe (Owner/Members) |
| `GET` | `/api/v1/projects/{projectId}/posts/archived` | ✅ 🔒 | Archivierte Posts (Owner/Members) |
| `POST` | `/api/v1/projects/{projectId}/posts` | ✅ 🔒 | Post anlegen (DRAFT/PUBLISHED) |
| `PUT` | `/api/v1/projects/{projectId}/posts/{postId}` | ✅ 🔒 | Post aktualisieren |
| `DELETE` | `/api/v1/projects/{projectId}/posts/{postId}` | ✅ 🔒 | Post löschen |
| `PATCH` | `/api/v1/projects/{projectId}/posts/{postId}/publish` | ✅ 🔒 | Post veröffentlichen |
| `PATCH` | `/api/v1/projects/{projectId}/posts/{postId}/archive` | ✅ 🔒 | Post archivieren |
| `POST` | `/api/v1/projects/{projectId}/posts/{postId}/image` | ✅ 🔒 | Post-Bild hochladen (`multipart`) |
| `GET` | `/api/v1/projects/{projectId}/posts/{postId}/image` | ✅ 🔒 | Post-Bild abrufen |

**DTOs:** `CreateProjectPostRequest` (`title`, `content`, `contentFormat`, `status`), `ProjectPostResponse`

---

### 4. Project GitHub Repo
**Base Path:** `/api/v1/projects/{projectId}/github-repo`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `PUT` | `/api/v1/projects/{projectId}/github-repo` | ✅ 🔒 | Repo verknüpfen (Owner) |
| `GET` | `/api/v1/projects/{projectId}/github-repo` | ✅ 🔒 | Verknüpfte Repo-Card |
| `DELETE` | `/api/v1/projects/{projectId}/github-repo` | ✅ 🔒 | Verknüpfung lösen (Owner) |
| `GET` | `/api/v1/projects/{projectId}/github-repo/readme` | ✅ 🔒 | README abrufen |
| `PATCH` | `/api/v1/projects/{projectId}/github-repo/readme-visibility` | ✅ 🔒 | README-Sichtbarkeit (Owner) |
| `PATCH` | `/api/v1/projects/{projectId}/github-repo/auto-invite` | ✅ 🔒 | Auto-Invite Collaborators (Owner) |

**DTOs:** `LinkGithubRepoRequest` (`repoOwner`, `repoName`), `SetReadmeVisibilityRequest` (`show`), `SetAutoInviteRequest` (`enabled`), `GithubRepoCardResponse`, `GithubReadmeResponse`

---

### 5. Project Invitations (direkte Einladungen)
**Base Path:** `/api/v1`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/projects/{projectId}/invitations` | ✅ 🔒 | Einladung erstellen (Owner) |
| `GET` | `/api/v1/projects/{projectId}/invitations` | ✅ 🔒 | Alle Einladungen eines Projekts (Owner) |
| `GET` | `/api/v1/users/me/invitations` | ✅ | Eigene Einladungen |
| `PATCH` | `/api/v1/invitations/{invitationId}` | ✅ 🔒 | Einladung annehmen/ablehnen |

**DTOs:** `CreateProjectInviteRequest` (`invitedUserId`, `message`), `UpdateProjectInviteStatusRequest` (`status`), `ProjectInviteResponse`

---

### 6. Project Join Requests (öffentliche Bewerbungen)
**Base Path:** `/api/v1`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/join-requests` | ✅ 🔒 | Join-Requests eines Projekts (Owner) |
| `POST` | `/api/v1/projects/{projectId}/join-requests` | ✅ 🔒 | Join-Request stellen |
| `GET` | `/api/v1/project-join-requests/me` | ✅ | Eigene Join-Requests |
| `PATCH` | `/api/v1/project-join-requests/{requestId}/accept` | ✅ 🔒 | Annehmen (Owner) |
| `PATCH` | `/api/v1/project-join-requests/{requestId}/reject` | ✅ 🔒 | Ablehnen (Owner) |

**DTOs:** `CreateProjectJoinRequestRequest` (`message`), `ProjectJoinRequestResponse`

---

### 7. Project Tags
**Base Path:** `/api/v1/projects/{projectId}/tags`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/tags` | ✅ 🔒 | Tags eines Projekts |
| `POST` | `/api/v1/projects/{projectId}/tags` | ✅ 🔒 | Tag hinzufügen (Owner) |
| `DELETE` | `/api/v1/projects/{projectId}/tags/{tagName}` | ✅ 🔒 | Tag entfernen (Owner) |

---

### 8. Profile Tags
**Base Path:** `/api/v1/users`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/users/{userId}/profile/tags` | ✅ | Tags eines Profils |
| `POST` | `/api/v1/users/me/profile/tags` | ✅ | Tag zum eigenen Profil hinzufügen |
| `DELETE` | `/api/v1/users/me/profile/tags/{tagName}` | ✅ | Tag vom eigenen Profil entfernen |

### 9. Tag-Verzeichnis
**Base Path:** `/api/v1/tags`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/tags?q=&limit=` | ✅ | Tags durchsuchen (Auto-Suggest) |

**DTOs (Tags):** `CreateTagRequest` (`name`), `TagResponse`

---

### 10. User Profiles
**Base Path:** `/api/v1/users` (Pfade teilweise inline im Controller)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/users/me` | ✅ | Eigenes Profil aus Keycloak synchronisieren/anlegen |
| `GET` | `/api/v1/users/{username}/profile` | ✅ | Profil nach Username |
| `PUT` | `/api/v1/users/{username}/profile` | ✅ 🔒 | Eigenes Profil aktualisieren |
| `DELETE` | `/api/v1/users/{username}/profile` | ✅ 🔒 | Eigenes Profil löschen |
| `GET` | `/api/v1/users/{username}/projects` | ✅ 🔒 | Projekte eines Users |
| `GET` | `/api/v1/users/{username}/projects/recent` | ✅ 🔒 | Letzte Projekte eines Users |
| `GET` | `/api/v1/users/{username}/projects/all` | ✅ 🔒 | Alle Projekte eines Users |
| `GET` | `/api/v1/users/{username}/theses` | ✅ 🔒 | Thesen eines Users |
| `GET` | `/api/v1/users/me/ban-status` | ✅ | Eigener Ban-Status |
| `PATCH` | `/api/v1/users/me/onboarding` | ✅ | Onboarding-Flag setzen |

**DTOs:** `UserProfileRequest` (`title`, `location`, `about`, `experience`, `placeId`), `UserProfileResponse`, `UserStatusResponse`, `UpdateOnboardingRequest` (`onboardingCompleted`)

---

### 11. User Management (Moderation)
**Base Path:** `/api/v1/users/management`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/users/management?status=` | ✅ 🔒 | Nutzer nach Status (paginiert) |
| `PATCH` | `/api/v1/users/management/{userId}/ban` | ✅ 🔒 | User bannen (`BanUserRequest.reason`) |
| `PATCH` | `/api/v1/users/management/{userId}/unban` | ✅ 🔒 | User entbannen |

**DTOs:** `ManagedUserResponse`, `BanUserRequest` (`reason`)

---

### 12. User Profile Links
**Base Path:** `/api/v1/users/{userId}/links`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/users/{userId}/links` | ✅ | Links eines Profils |
| `POST` | `/api/v1/users/{userId}/links` | ✅ 🔒 | Link anlegen (Owner) |
| `PATCH` | `/api/v1/users/{userId}/links/{linkId}` | ✅ 🔒 | Link aktualisieren (Owner) |
| `DELETE` | `/api/v1/users/{userId}/links/{linkId}` | ✅ 🔒 | Link löschen (Owner) |

### Project Links
**Base Path:** `/api/v1/projects/{projectId}/links`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/links` | ✅ 🔒 | Links eines Projekts |
| `POST` | `/api/v1/projects/{projectId}/links` | ✅ 🔒 | Link anlegen (Owner) |
| `PATCH` | `/api/v1/projects/{projectId}/links/{linkId}` | ✅ 🔒 | Link aktualisieren (Owner) |
| `DELETE` | `/api/v1/projects/{projectId}/links/{linkId}` | ✅ 🔒 | Link löschen (Owner) |

**DTOs:** `CreateProjectLinkRequest`/`UpdateProjectLinkRequest` (`label`, `url`, `visibility`), `ProjectLinkResponse`; `CreateUserProfileLinkRequest`/`UpdateUserProfileLinkRequest` (`label`, `url`), `UserProfileLinkResponse`

---

### 13. User Follow
**Base Path:** `/api/v1/users`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/users/{username}/followers` | ✅ | User folgen |
| `DELETE` | `/api/v1/users/{username}/followers` | ✅ | Folgen aufheben |
| `GET` | `/api/v1/users/{username}/followers/me` | ✅ | Folgt der aktuelle User? |
| `GET` | `/api/v1/users/{username}/followers` | ✅ | Follower-Liste |
| `GET` | `/api/v1/users/{username}/following` | ✅ | Following-Liste |

---

### 14. Project Favorites
**Base Path:** `/api/v1/users/me/favorites`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/users/me/favorites` | ✅ | Eigene Favoriten |
| `GET` | `/api/v1/users/me/favorites/{projectId}` | ✅ | Favorisiert? |
| `POST` | `/api/v1/users/me/favorites/{projectId}` | ✅ 🔒 | Favorit hinzufügen |
| `DELETE` | `/api/v1/users/me/favorites/{projectId}` | ✅ | Favorit entfernen |

---

### 15. Theses
**Base Path:** `/api/v1/theses`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/theses` | ✅ 🔒 | Alle Thesen (paginiert, Filter `title`) |
| `POST` | `/api/v1/theses` | ✅ 🔒 | These anlegen |
| `GET` | `/api/v1/theses/{thesisId}` | ✅ 🔒 | These nach ID |
| `PUT` | `/api/v1/theses/{thesisId}` | ✅ 🔒 | These aktualisieren |
| `DELETE` | `/api/v1/theses/{thesisId}` | ✅ 🔒 | These löschen |
| `GET` | `/api/v1/theses/by-url/{thesisUrl}` | ✅ 🔒 | These nach URL |
| `GET` | `/api/v1/theses/{thesisId}/students` | ✅ 🔒 | Studierende der These |
| `POST` | `/api/v1/theses/{thesisId}/students/{studentKeycloakId}` | ✅ 🔒 | Studierenden zuordnen |
| `DELETE` | `/api/v1/theses/{thesisId}/students/{studentKeycloakId}` | ✅ 🔒 | Studierende entfernen |
| `GET` | `/api/v1/theses/url-exists/{thesisUrl}` | ✅ | URL vergeben? |
| `GET` | `/api/v1/theses/students/{studentKeycloakId}/already-assigned` | ✅ 🔒 | Studierende schon zugewiesen? |

**DTOs:** `CreateThesisRequest` (`title`, `description`, `shortDescription`, `thesisUrl`, `tags`), `UpdateThesisRequest`, `ThesisResponse`, `DeleteThesisResponse`, `ThesisStudentResponse`

### Thesis Assignment Notifications
**Base Path:** `/api/v1/users/me/thesis-notifications`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/users/me/thesis-notifications/unread-count` | ✅ | Ungelesene Zuweisungs-Notifications |
| `POST` | `/api/v1/users/me/thesis-notifications/mark-read` | ✅ | Alle als gelesen markieren |

**DTOs:** `UnreadThesisNotificationCountResponse`

---

### 16. Professor Requests
**Base Path:** `/api/v1/professor-requests`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/professor-requests` | ✅ 🔒 | Alle Anträge (paginiert, Admin) |
| `GET` | `/api/v1/professor-requests/{userId}` | ✅ 🔒 | Anträge eines Users |
| `POST` | `/api/v1/professor-requests` | ✅ 🔒 | Antrag stellen |
| `POST` | `/api/v1/professor-requests/verify` | ❌ | THM-E-Mail verifizieren (Token) |
| `PATCH` | `/api/v1/professor-requests/{requestId}/accept` | ✅ 🔒 | Annehmen (Admin) |
| `PATCH` | `/api/v1/professor-requests/{requestId}/reject` | ✅ 🔒 | Ablehnen (Admin) |

**DTOs:** `CreateProfessorRequestRequest` (`email`, `text`), `VerifyProfessorRequestEmailRequest` (`token`), `ProfessorRequestResponse`

---

### 17. Reports (Moderation)
**Base Path:** `/api/v1/reports`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/reports` | ✅ 🔒 | Reports filtern/suchen (paginiert, Moderator) |
| `POST` | `/api/v1/reports` | ✅ 🔒 | Report erstellen |
| `PATCH` | `/api/v1/reports/{reportId}/status` | ✅ 🔒 | Status ändern (Moderator) |
| `PATCH` | `/api/v1/reports/targets/{target}/{targetId}/resolve-active` | ✅ 🔒 | Alle offenen Reports zum Target auflösen |

**DTOs:** `CreateReportRequest` (`target`, `targetId`, `reason`, `message`), `ReportResponse`, `ModeratorReportResponse`, `UpdateReportStatusRequest` (`status`, `moderatorMessage`), `ResolveReportsForTargetRequest` (`moderatorMessage`)

---

### 18. Audit Logs
**Base Path:** `/api/v1/audit-logs`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/audit-logs` | ✅ 🔒 | Audit-Logs (paginiert, Admin) |

**DTOs:** `AuditLogResponse`

---

### 19. Search
**Base Path:** `/api/v1/search`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/search/projects/paged` | ✅ | Projektsuche (paginiert, Filter) |
| `GET` | `/api/v1/search/users` | ✅ | Usersuche (Liste) |
| `GET` | `/api/v1/search/users/paged` | ✅ | Usersuche (paginiert, Filter) |

> `q` ist mehrfach erlaubt (AND-Logik). Project-Filter: `hasOpenPositions`, `allowJoinRequests`, `tags`, `createdAfter`, `createdBefore`. User-Filter: `isProfessor`, `tags`, `location`, `createdAfter`, `createdBefore`. Gebannte User werden ausgeschlossen. Die nicht-paginierte `/projects`-Suche ist deprecated und auskommentiert.

**DTOs:** `ProjectSearchResult`, `ProjectSearchFilter`, `UserSearchResult`, `UserSearchFilter`

---

### 20. Discord Integration
**OAuth:** `/api/v1/auth/discord`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/auth/discord/authorize` | ✅ | OAuth-Authorize-URL holen |
| `GET` | `/api/v1/auth/discord/callback` | ❌ | OAuth-Callback (User + Bot-Flow) |
| `DELETE` | `/api/v1/auth/discord/disconnect` | ✅ | Discord-Konto trennen |

**Channel/Guild:** `/api/v1/projects/{projectId}/discord`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/discord/connect` | ✅ 🔒 | Aktuelle Channel-Verknüpfung |
| `POST` | `/api/v1/projects/{projectId}/discord/connect` | ✅ 🔒 | Channel verknüpfen (`channelId`, `guildId`) |
| `POST` | `/api/v1/projects/{projectId}/discord/auto-connect` | ✅ 🔒 | Auto-Connect Channel |
| `DELETE` | `/api/v1/projects/{projectId}/discord/connect` | ✅ 🔒 | Channel-Verknüpfung lösen |
| `PATCH` | `/api/v1/projects/{projectId}/discord/invite` | ✅ 🔒 | Discord-Invite-URL setzen |
| `GET` | `/api/v1/projects/{projectId}/discord/bot-invite` | ✅ 🔒 | Bot-Invite-URL |
| `GET` | `/api/v1/projects/{projectId}/discord/guilds` | ✅ 🔒 | Verfügbare Guilds |

**Settings:** `/api/v1/projects/{projectId}/discord/settings`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/discord/settings` | ✅ 🔒 | Notify-Settings lesen |
| `PUT` | `/api/v1/projects/{projectId}/discord/settings` | ✅ 🔒 | Notify-Settings schreiben |

**Status:** `/api/v1/projects/{projectId}/discord/status`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/projects/{projectId}/discord/status` | ✅ 🔒 | Discord-Verbindungsstatus |

**DTOs:** `DiscordChannelResponse`, `DiscordSettingsResponse` (+ `UpdateRequest`), `DiscordStatusResponse`; Guild-Info aus `BotInternalClient.GuildInfo`

---

### 21. GitHub Integration
**Base Path:** `/api/v1/github/connection`

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/github/connection` | ✅ 🔒 | Verbindungsstatus |
| `POST` | `/api/v1/github/connection/authorize-url` | ✅ 🔒 | OAuth-Authorize-URL |
| `POST` | `/api/v1/github/connection/callback` | ✅ 🔒 | OAuth-Callback (`code`, `state`) |
| `DELETE` | `/api/v1/github/connection` | ✅ 🔒 | Verbindung trennen |

**DTOs:** `GithubAuthorizeUrlResponse`, `GithubCallbackRequest` (`code`, `state`), `GithubConnectionStatusResponse`

---

## 🔐 Security & Authentication

- **Framework:** Spring Security + OAuth2 Resource Server (JWT Bearer)
- **Auth Provider:** Keycloak (`issuer-uri` in `application.yaml`)
- **Method Security:** `@EnableMethodSecurity` + `@PreAuthorize("@security.…")` für feingranulare Rechte (`SecurityService`)
- **Stateless:** `SessionCreationPolicy.STATELESS`, CSRF deaktiviert
- **Banned-User-Filter:** `BannedUserFilter` blockiert gebannte User nach Auth
- **CORS:** erlaubte Origins `*.swtp-ss26.de`, `*.review.swtp-ss26.de`, `http://localhost:4200`

Öffentlich (`permitAll`): `/api/public/**`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `POST /api/v1/professor-requests/verify`, `GET /api/v1/auth/discord/callback`. Alle anderen Requests erfordern JWT.

**Config-Klassen:** `SecurityConfig`, `KeycloakJwtConverter`, `SecurityService`, `BannedUserFilter`, `CacheConfig`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`

---

## 📦 Technology Stack

**Backend:**
- Spring Boot 4.0.6 / Java 25
- Spring Data JPA (Hibernate), SQLite-Dialect (dev) / MySQL (prod-Profil `application-mysql.yaml`)
- Redis (Caching)
- Spring Mail
- OAuth2 Resource Server (Keycloak JWT)
- Lombok

**Build & Quality:**
- Maven
- Checkstyle

---

## 🛠️ Error Handling

Globales Exception-Handling über `GlobalExceptionHandler.java` liefert standardisierte `ErrorResponse`-Objekte. Exceptions sind überwiegend in `exceptionhandling/exceptions/` zentralisiert, modulspezifische liegen im jeweiligen Modul-`exception`-Package.

**Project:** `ProjectNotFoundException`, `ProjectNotFoundByUrlException`, `ExceptionProjectNotFound`, `ExceptionProjectNameAlreadyExists`, `ExceptionProjectUrlAlreadyExists`, `ExceptionInvalidProjectUrl`, `ExceptionProjectUrlGenerationFailed`, `ExceptionProjectEditNotAllowed`, `ExceptionProjectDeleteNotAllowed`, `ExceptionProjectAlreadyDeleted`, `ExceptionOwnerNotFound`, `ProjectOwnerTransferToSelfException`, `ProjectOwnerTransferToNonMemberException`, `ProjectMemberNotFoundException`, `ProjectOwnerCannotBeRemovedException`

**User Profile:** `UserProfileNotFoundException`, `ProfileAccessDeniedException`

**User Management:** `InvalidUserManagementSortFieldException`, `InvalidProjectManagementSortFieldException`

**Tags:** `TagAccessDeniedException`, `TagNotValidException`, `TagSourceApiException`

**Links:** `ProjectLinkNotFoundException`, `ProjectLinkAlreadyExistsException`, `ProjectLinkDoesNotBelongToProjectException`, `UserProfileLinkNotFoundException`, `UserProfileLinkAlreadyExistsException`, `UserProfileLinkDoesNotBelongToProfileException`, `UserProfileLinkEditNotAllowedException`

**Files:** `ProjectFileNotFoundException`, `ProjectFileDoesNotBelongToProjectException`, `ProjectFileTypeNotAllowedException`, `ProjectFileUploadLimitExceededException`

**Posts:** `ProjectPostNotFoundException`, `ProjectPostAccessDeniedException`, `InvalidProjectPostException`

**Invitations:** `ProjectInviteNotFoundException`, `ProjectInviteAccessDeniedException`, `InvalidProjectInviteException`

**Join Requests:** `ProjectJoinRequestNotFoundException`, `ProjectJoinRequestAccessDeniedException`, `ProjectJoinRequestAlreadyExistsException`, `ProjectJoinRequestInvalidStatusForEditException`

**Favorites:** `ProjectAlreadyFavoritedException`, `ProjectFavoriteNotFoundException`

**Follow:** `CannotFollowYourselfException`, `UserAlreadyFollowingException`, `UserFollowNotFoundException`

**Thesis:** `ThesisNotFoundException`, `ThesisNotFoundByIdException`, `ThesisTitleAlreadyExistsException`, `ThesisUrlAlreadyExistsException`, `ThesisUrlGenerationFailedException`, `ThesisInvalidUrlException`, `ThesisStudentNotFoundException`, `ThesisStudentAlreadyAssignedException`, `ThesisStudentAlreadyAssignedElsewhereException`, `ThesisInvalidStudentAssignmentException`

**Professor Requests:** `ProfessorRequestNotFoundException`, `ProfessorRequestAlreadyExistsException`, `ProfessorRequestInvalidStatusException`, `InvalidProfessorEmailDomainException`

**Reports:** `ReportNotFoundException`, `ReportAlreadyExistsException`, `ReportTargetNotFoundException`, `InvalidReportStatusException`, `InvalidReportTargetException`, `InvalidReportSortFieldException`

**Audit Logs:** `InvalidAuditLogSortFieldException`

**Discord:** `DiscordAccountAlreadyLinkedException`, `DiscordChannelNotFoundException`, `DiscordConnectionFailedException`

**GitHub:** `GithubApiException`, `GithubOAuthException`, `GithubConnectionRequiredException`, `GithubIntegrationDisabledException`, `GithubTokenInvalidException`, `InvalidGithubStateException`, `GithubRepoNotFoundException`, `GithubRepoNotLinkedException`, `GithubRepoAccessDeniedException`, `GithubReadmeNotEnabledException`

**Location / Moderation:** `GooglePlacesApiException`, `InvalidPlaceException`, `ContentModerationException`, `ContentNotValidException`, `ModerationApiException`

---

## 📚 Module Overview

| Module | Purpose | Key Classes |
|--------|---------|------------|
| **controller** | Hello/Health | `HelloController` |
| **project** | Project CRUD, Members, Ownership | `ProjectController`, `ProjectService`, `ProjectRepository` |
| **projectFiles** | Datei-Uploads/-Downloads pro Projekt | `ProjectFileController`, `ProjectFileService` |
| **projectPost** | Posts/Ankündigungen | `ProjectPostController`, `ProjectPostService` |
| **projectGithubRepo** | GitHub-Repo-Verknüpfung + README | `ProjectGithubRepoController`, `ProjectGithubRepoService` |
| **projectInvitation** | Direkte Einladungen | `ProjectInviteController`, `ProjectInviteService` |
| **projectJoinRequest** | Öffentliche Bewerbungen | `ProjectJoinRequestController`, `ProjectJoinRequestService` |
| **projectFavorite** | Favoriten/Likes | `ProjectFavoriteController`, `ProjectFavoriteService` |
| **projectView** | View-Counter | `ProjectViewService`, `ProjectViewRepository` |
| **userprofile** | Profile, Onboarding | `UserProfileController`, `UserProfileService` |
| **userprofile (mgmt)** | User-Management/Ban | `UserManagementController` |
| **userFollow** | Follow/Follower | `UserFollowController`, `UserFollowService` |
| **tag** | Tags (Projekt/Profile/Verzeichnis) | `ProjectTagController`, `ProfileTagController`, `TagController` |
| **links** | Project- & Profile-Links | `ProjectLinkController`, `UserProfileLinkController` |
| **thesis** | Abschlussarbeiten + Students | `ThesisController`, `ThesisService`, `ThesisAssignmentNotificationController` |
| **professorRequest** | Professor-Rechte-Anträge | `ProfessorRequestController`, `ProfessorRequestService` |
| **reports** | Meldungen/Moderation | `ReportController`, `ReportService` |
| **auditlog** | Audit-Logs | `AuditLogController`, `AuditLogService` |
| **search** | Volltextsuche | `SearchController`, `ProjectSearchService`, `UserSearchService` |
| **discord** | Discord OAuth, Channels, Settings, Status | `DiscordAuthController`, `DiscordChannelController`, `DiscordSettingsController`, `DiscordStatusController` |
| **github** | GitHub OAuth, Connection | `GithubConnectionController`, `GithubConnectionService` |
| **notification** | Event-basierte Notifications | Listener/Events unter `notification/` |
| **moderation** | Content-Moderation (Helpers) | `ContentModerationException`, `ContentNotValidException` |
| **location** | Google Places Hilfscode | Location-Exceptions |
| **config** | Security/OAuth2/Caching | `SecurityConfig`, `KeycloakJwtConverter`, `SecurityService`, `BannedUserFilter`, `CacheConfig` |
| **exceptionhandling** | Globales Fehlerhandling | `GlobalExceptionHandler`, `ErrorResponse` |
| **common** | Shared Utilities | `PageResponse` |

---

*Endpunkte und DTOs direkt aus den Controllern/`dto`-Packages extrahiert. Bei Änderungen an der Codebase diese Doku bitte nachpflegen.*