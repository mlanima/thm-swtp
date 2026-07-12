<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/ideacamp-logo-dark.png">
  <img src="docs/assets/ideacamp-logo-light.png" alt="IdeaCamp" width="360">
</picture>

**Kollaborative Plattform zum Vorschlagen, Entdecken und Verwalten von Projektideen.**

Gruppe 3 · *Software Engineering: Realisierung* · THM

[Dokumentation](#dokumentation) · [Umgebungen](#umgebungen) · [Mitwirkende](#mitwirkende)

[![CI Backend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml)
[![CI Frontend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml)

</div>

## Über das Projekt

IdeaCamp ermöglicht es Studierenden und Mitarbeitenden, Projektideen einzureichen, zu durchsuchen und gemeinsam daraus echte Projekte zu machen. Authentifizierung und Rollen laufen zentral über Keycloak (OAuth2 / OIDC); jeder Pull Request erhält automatisch eine eigene Review-Umgebung.

## Tech-Stack

<div align="center">

[![Java][Java-badge]][Java-url]
[![Spring Boot][SpringBoot-badge]][SpringBoot-url]
[![Maven][Maven-badge]][Maven-url]

[![Angular][Angular-badge]][Angular-url]
[![TypeScript][TypeScript-badge]][TypeScript-url]
[![Tailwind CSS][Tailwind-badge]][Tailwind-url]
[![Zod][Zod-badge]][Zod-url]

[![Keycloak][Keycloak-badge]][Keycloak-url]
[![MySQL][MySQL-badge]][MySQL-url]
[![SQLite][SQLite-badge]][SQLite-url]

[![Docker][Docker-badge]][Docker-url]
[![Traefik][Traefik-badge]][Traefik-url]
[![GitHub Actions][Actions-badge]][Actions-url]

</div>

MySQL 9 in Produktion, SQLite für die lokale Entwicklung. Deployment über Docker Compose hinter Traefik; GitHub Actions übernimmt Build, Lint, Tests und Auto-Deploy inklusive Review-Apps.

## Erste Schritte

Voraussetzungen: **Java 25**, **Node.js 20+** (npm 11+), optional **Docker**.

```bash
git clone git@github.com:mlanima/thm-swtp.git
cd thm-swtp
```

### Backend

```bash
cd api
./mvnw spring-boot:run
```

Läuft auf `http://localhost:8080` mit lokaler SQLite-Datenbank. Keycloak (Realm `swtp`) wird unter `https://auth.swtp-ss26.de` erwartet — bei Bedarf in `src/main/resources/application.yaml` anpassen.

<details>
<summary>Alternativ mit Docker</summary>

```bash
cd api
docker build -t swtp-api .
docker run -p 8080:8080 -v $(pwd)/db:/app/db swtp-api   # Windows: ${PWD}
```

</details>

### Frontend

```bash
cd web/ideacamp
npm install
npm start
```

Läuft auf `http://localhost:4200`, API-Aufrufe werden an Port 8080 weitergeleitet. Weitere Skripte: `npm run build`, `npm test`, `npm run lint`.

## Projektstruktur

```
thm-swtp/
├── api/            # Spring-Boot-Backend (REST-API, Keycloak-gesichert)
├── web/ideacamp/   # Angular-Frontend (SSR)
├── infra/          # Compose-Stacks, Traefik, Deploy-Skripte
├── docs/           # Architektur- & Integrationsdokumentation
└── .github/        # CI/CD-Workflows
```

## Umgebungen

| Umgebung | Branch | URL |
|---|---|---|
| Produktion | `main` | [www.swtp-ss26.de](https://www.swtp-ss26.de) |
| Entwicklung | `developer` | [dev.swtp-ss26.de](https://dev.swtp-ss26.de) |
| Review-Apps | pro PR | automatisch erstellt, beim Schließen abgebaut |
| Status | — | [status.swtp-ss26.de](https://status.swtp-ss26.de) |

## Dokumentation

- [Backend-API](docs/BACKEND_API_DOCUMENTATION.md)
- [Frontend-Struktur & Auth](docs/FRONTEND_STRUCTURE_AND_AUTH.md)
- [Keycloak — Setup & Konfiguration](docs/Keycloak%20-%20Setup%20&%20Konfiguration.md)
- [Rollen](docs/ROLLEN.md)
- [Berechtigungen](docs/permissions.md)
- [Datenbank Schema](docs/database-schema.md)
- [Caching](docs/CACHING.md)
- [CI/CD & Deployment](docs/CI_CD.md)
- [Inhaltsmoderation](docs/CONTENT_MODERATION.md)
- [Tag Moderation](docs/TAG_VALIDATION.md)
- [Discord Integration](docs/DISCORD_INTEGRATION.md)
- [GitHub Integration](docs/GITHUB_INTEGRATION.md)
- [Scrum Dokumentation](docs/SCRUM.md)


## Mitwirkende

<div align="center">

<a href="https://github.com/mlanima/thm-swtp/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=mlanima/thm-swtp" alt="Mitwirkende" />
</a>

<br/><br/>

<sub>THM · Software Engineering: Realisierung · Gruppe 3 · SS26</sub>

</div>

<!-- BADGES -->
[Java-badge]: https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://openjdk.org/
[SpringBoot-badge]: https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[SpringBoot-url]: https://spring.io/projects/spring-boot
[Maven-badge]: https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white
[Maven-url]: https://maven.apache.org/
[Angular-badge]: https://img.shields.io/badge/Angular_21-DD0031?style=for-the-badge&logo=angular&logoColor=white
[Angular-url]: https://angular.dev/
[TypeScript-badge]: https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white
[TypeScript-url]: https://www.typescriptlang.org/
[Tailwind-badge]: https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white
[Tailwind-url]: https://tailwindcss.com/
[Zod-badge]: https://img.shields.io/badge/Zod-3E67B1?style=for-the-badge&logo=zod&logoColor=white
[Zod-url]: https://zod.dev/
[Keycloak-badge]: https://img.shields.io/badge/Keycloak-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white
[Keycloak-url]: https://www.keycloak.org/
[MySQL-badge]: https://img.shields.io/badge/MySQL_9-4479A1?style=for-the-badge&logo=mysql&logoColor=white
[MySQL-url]: https://www.mysql.com/
[SQLite-badge]: https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white
[SQLite-url]: https://sqlite.org/
[Docker-badge]: https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white
[Docker-url]: https://www.docker.com/
[Traefik-badge]: https://img.shields.io/badge/Traefik-24A1C1?style=for-the-badge&logo=traefikproxy&logoColor=white
[Traefik-url]: https://traefik.io/
[Actions-badge]: https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white
[Actions-url]: https://github.com/features/actions
