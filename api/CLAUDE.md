# Backend (api/)

Spring Boot application, Java 25, built with Maven.

- Modules used: Spring Data JPA, Spring Security.
- Style: enforced via Checkstyle (`checkstyle.xml`).
- Local DB: SQLite (`identifier.sqlite`) for dev/test; production uses MySQL (profile `mysql`).
- File uploads: `ProjectFileService` / `ProjectFileController` under `projectFiles/`. Upload
  directory is provided via `APP_UPLOADS_DIR` env var and mounted as a host bind mount.

## Logging conventions

- Logger: Lombok `@Slf4j` on the service/handler class. No `System.out`, no manual `LoggerFactory`.
- Placeholders over concatenation: `log.info("Project created: id={}", id)`, never string concat.
- Levels:
  - `error` — unexpected, technical failure (e.g. `GlobalExceptionHandler` catch-all, IO errors).
  - `warn` — expected but notable: denied ownership/access, business-rule violations, orphaned state.
  - `info` — lifecycle events: create/update/delete/status transitions, with resource id (+ actor where available).
  - `debug` — query parameters & hit counts (search); off in production, no prod noise.
- `GlobalExceptionHandler` level matrix: `AccessDenied`/`Forbidden` and server-enforced business-policy 4xx (file-type 415, upload-limit 422) → `warn`; expected client 4xx (404/409/400/410) → `debug`; 5xx and catch-all → `error`.
- Division of labour: services log business/Fachfehler; `GlobalExceptionHandler` logs technical/unexpected exceptions once. Domain exceptions are not re-logged in the handler.
- Read paths stay silent unless the log answers an incident question.

## Review focus

- Security: authentication/authorization (Keycloak integration), input validation, SQL/JPA usage.
- Checkstyle compliance and existing package/naming conventions under `src/`.
- Bug risk in business logic and error handling.
- Avoid suggesting unrelated framework migrations or large refactors.
