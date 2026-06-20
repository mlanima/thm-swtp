# THM SWTP – IdeaCamp

Monorepo for the SWTP project "IdeaCamp":

- `api/` – Spring Boot backend (Java 25, Maven). See `api/CLAUDE.md`.
- `web/ideacamp/` – Angular frontend. See `web/ideacamp/CLAUDE.md`.
- `infra/` – Deployment infrastructure (Docker Compose stacks, Traefik, review apps).
- `docs/` – Architecture and integration docs (auth/Keycloak, backend API, frontend structure).

## Code review notes

When reviewing pull requests:
- Focus on correctness, security, and consistency with existing conventions in the touched module.
- Match feedback to the sub-project's CLAUDE.md (backend vs. frontend) where relevant.
- Don't flag infra/`.bb` deploy scripts or dashboard files for frontend/backend conventions — they're standalone tooling.
