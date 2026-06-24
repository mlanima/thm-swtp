# Backend (api/)

Spring Boot application, Java 25, built with Maven.

- Modules used: Spring Data JPA, Spring Security.
- Style: enforced via Checkstyle (`checkstyle.xml`).
- Local DB: SQLite (`identifier.sqlite`) for dev/test; production uses MySQL (profile `mysql`).
- File uploads: `ProjectFileService` / `ProjectFileController` under `projectFiles/`. Upload
  directory is provided via `APP_UPLOADS_DIR` env var and mounted as a host bind mount.

## Logging conventions

- Lombok `@Slf4j` on the service/handler. No `System.out`, no manual `LoggerFactory`. Placeholders over concatenation.
- Levels: `error` unexpected/technical (catch-all, IO); `warn` denied access, business-rule violations, orphaned state; `info` lifecycle (create/update/delete/status) with resource id; `debug` query params & hit counts (search), off in prod.
- `GlobalExceptionHandler` maps by status: 403/413/415/422 → `warn`; 400/404/409/410 → `debug`; 5xx → `error`. Services log Fachfehler, the handler logs technical/unexpected once; domain exceptions aren't re-logged.
- 403 is uniform: `GlobalExceptionHandler` (method-security) and `RestAccessDeniedHandler` (URL-based, filter chain) both return the same generic body and log at `warn`.
- Free-text that reaches a log line is sanitized via `LogSafe.clean`: search queries, upload filenames/Content-Type, and handler messages embedding raw request input (project name, project URL, JSON-deserialization errors). Other `ex.getMessage()` is logged unstripped (UUID/enums/validation — no CRLF survives).
- Reads stay silent unless a log answers an incident question. Dev visibility: `BE_LOG_LEVEL=DEBUG` (→ `logging.level.de.thm.swtp`) turns on `RequestLoggingFilter` + search debug; prod stays INFO.

## Review focus

- Security: authentication/authorization (Keycloak integration), input validation, SQL/JPA usage.
- Checkstyle compliance and existing package/naming conventions under `src/`.
- Bug risk in business logic and error handling.
- Avoid suggesting unrelated framework migrations or large refactors.
