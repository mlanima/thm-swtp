<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/ideacamp-logo-dark.png">
  <img src="docs/assets/ideacamp-logo-light.png" alt="IdeaCamp" width="360">
</picture>

**Kollaborative Plattform zum Vorschlagen, Entdecken und Verwalten von Projektideen.**

Gruppe 3 · *Software Engineering: Realisierung* · THM

[Dokumentation](#dokumentation) · [Umgebungen](#umgebungen) · [Mitwirken](#mitwirken)

[![CI Backend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml)
[![CI Frontend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml)

</div>

## Über das Projekt

IdeaCamp ermöglicht es Studierenden und Mitarbeitenden, Projektideen einzureichen, zu durchsuchen und gemeinsam daraus echte Projekte zu machen. Authentifizierung und Rollen laufen zentral über Keycloak (OAuth2 / OIDC); jeder Pull Request erhält automatisch eine eigene Review-Umgebung.

## Tech-Stack

| Ebene | Technologie |
|---|---|
| Backend | Java 25, Spring Boot (Web, Data JPA, Security), Maven |
| Frontend | Angular 21 (SSR), TypeScript, Tailwind CSS, Zod, ngx-translate |
| Auth | Keycloak (OAuth2 / OIDC) |
| Datenbank | MySQL 9 (Produktion), SQLite (lokal) |
| Infrastruktur | Docker Compose, Traefik, Babashka-Deploy-Skripte |
| CI/CD | GitHub Actions — Build, Lint, Tests, Auto-Deploy, Review-Apps |

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
- [Rollen & Berechtigungen](docs/ROLLEN.md)
- [Caching](docs/CACHING.md)

## Mitwirken

Feature-Branch von `developer` erstellen, Pull Request gegen `developer` öffnen. Die CI prüft Checkstyle, ESLint, Builds und Tests; eine Review-App wird automatisch deployt, sodass Änderungen live begutachtet werden können.

## Mitwirkende

<a href="https://github.com/mlanima"><img src="https://github.com/mlanima.png" width="56" alt="mlanima"/></a>
<a href="https://github.com/chrishnz"><img src="https://github.com/chrishnz.png" width="56" alt="chrishnz"/></a>
<a href="https://github.com/T0SCH"><img src="https://github.com/T0SCH.png" width="56" alt="T0SCH"/></a>
<a href="https://github.com/dsmk-cpu"><img src="https://github.com/dsmk-cpu.png" width="56" alt="dsmk-cpu"/></a>
<a href="https://github.com/KSMEHMET42"><img src="https://github.com/KSMEHMET42.png" width="56" alt="KSMEHMET42"/></a>
<a href="https://github.com/halitcinar"><img src="https://github.com/halitcinar.png" width="56" alt="halitcinar"/></a>

<div align="center">
<sub>THM · Software Engineering: Realisierung · Gruppe 3 · SS26</sub>
</div>
