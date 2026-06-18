# Backend (api/)

Spring Boot application, Java 25, built with Maven.

- Modules used: Spring Data JPA, Spring Security.
- Style: enforced via Checkstyle (`checkstyle.xml`).
- Local DB: SQLite (`identifier.sqlite`) for dev/test.

## Review focus

- Security: authentication/authorization (Keycloak integration), input validation, SQL/JPA usage.
- Checkstyle compliance and existing package/naming conventions under `src/`.
- Bug risk in business logic and error handling.
- Avoid suggesting unrelated framework migrations or large refactors.
