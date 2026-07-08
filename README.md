<div align="center">

# 💡 IdeaCamp

**A collaborative platform for pitching, discovering and managing project ideas.**

Built by Group 3 for the *Software Engineering: Realisierung* course at THM (Technische Hochschule Mittelhessen).

[![CI Backend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-backend.yml)
[![CI Frontend](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml/badge.svg)](https://github.com/mlanima/thm-swtp/actions/workflows/ci-frontend.yml)

![Java](https://img.shields.io/badge/Java%2025-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular%2021-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![Keycloak](https://img.shields.io/badge/Keycloak-4D4D4D?logo=keycloak&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)

</div>

---

## 📑 Table of Contents

- [About](#-about)
- [Built With](#-built-with)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend](#backend-api)
  - [Frontend](#frontend-webideacamp)
- [Project Structure](#-project-structure)
- [Environments](#-environments)
- [Documentation](#-documentation)
- [Contributing](#-contributing)
- [Contributors](#-contributors)

## 🎯 About

IdeaCamp lets students and staff propose project ideas, browse and search existing ones, and collaborate on turning them into real projects. Authentication and role management are handled centrally via Keycloak (OAuth2 / OIDC), and every pull request gets its own fully deployed review environment.

## 🛠 Built With

| Layer | Technology |
|---|---|
| **Backend** | Java 25, Spring Boot (Web, Data JPA, Security), Maven, Checkstyle |
| **Frontend** | Angular 21 (SSR via `@angular/ssr` + Express), TypeScript, Tailwind CSS, Zod, ngx-translate |
| **Auth** | Keycloak (OAuth2 / OIDC), `angular-oauth2-oidc` |
| **Database** | MySQL 9 (production), SQLite (local dev/test) |
| **Infrastructure** | Docker Compose, Traefik (TLS via Let's Encrypt), Babashka deploy scripts |
| **CI/CD** | GitHub Actions — build, lint, test, auto-deploy & per-PR review apps |

## 🚀 Getting Started

### Prerequisites

- **Java 25** (backend)
- **Node.js 20+** with npm 11+ (frontend)
- **Docker** (optional, for containerized runs)

Clone the repository:

```bash
git clone git@github.com:mlanima/thm-swtp.git
cd thm-swtp
```

### Backend (`api/`)

```bash
cd api
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080` using a local SQLite database. Keycloak (realm `swtp`) is expected at `https://auth.swtp-ss26.de` — adjust `src/main/resources/application.yaml` if needed.

<details>
<summary>Run with Docker instead</summary>

```bash
cd api
docker build -t swtp-api .
docker run -p 8080:8080 -v $(pwd)/db:/app/db swtp-api   # Windows: use ${PWD}
```

</details>

### Frontend (`web/ideacamp/`)

```bash
cd web/ideacamp
npm install
npm start
```

The app runs at `http://localhost:4200` and proxies API calls to the backend on port 8080.

Other useful scripts:

```bash
npm run build                  # production build
npm run serve:ssr:ideacamp     # serve the SSR build
npm test                       # unit tests (Vitest)
npm run lint                   # ESLint
```

## 📁 Project Structure

```
thm-swtp/
├── api/            # Spring Boot backend (REST API, Keycloak-secured)
├── web/ideacamp/   # Angular frontend (SSR)
├── infra/          # Docker Compose stacks, Traefik config, deploy scripts
├── docs/           # Architecture & integration docs
└── .github/        # CI/CD workflows
```

Each sub-project has its own README with more detail.

## 🌍 Environments

| Environment | Branch | URL |
|---|---|---|
| **Production** | `main` | [www.swtp-ss26.de](https://www.swtp-ss26.de) |
| **Development** | `developer` | [dev.swtp-ss26.de](https://dev.swtp-ss26.de) |
| **Review apps** | per PR | spun up automatically, torn down on close |
| **Status dashboard** | — | [status.swtp-ss26.de](https://status.swtp-ss26.de) |

Pushes to `main` and `developer` are deployed automatically via GitHub Actions. Every pull request additionally gets its own isolated environment (dedicated database schema, containers and Keycloak client).

## 📚 Documentation

- [Backend API documentation](docs/BACKEND_API_DOCUMENTATION.md)
- [Frontend structure & auth](docs/FRONTEND_STRUCTURE_AND_AUTH.md)
- [Keycloak setup & configuration](docs/Keycloak%20-%20Setup%20&%20Konfiguration.md)
- [Roles & permissions](docs/ROLLEN.md)
- [Caching](docs/CACHING.md)

## 🤝 Contributing

1. Create a feature branch from `developer` (e.g. `feat/my-feature`).
2. Commit your changes — CI runs Checkstyle, ESLint, builds and tests on every PR.
3. Open a pull request against `developer`. A review app is deployed automatically so reviewers can try your changes live.
4. Get a review and merge. 🎉

## 👥 Contributors

<a href="https://github.com/mlanima"><img src="https://github.com/mlanima.png" width="60" style="border-radius:50%" alt="mlanima"/></a>
<a href="https://github.com/chrishnz"><img src="https://github.com/chrishnz.png" width="60" style="border-radius:50%" alt="chrishnz"/></a>
<a href="https://github.com/T0SCH"><img src="https://github.com/T0SCH.png" width="60" style="border-radius:50%" alt="T0SCH"/></a>
<a href="https://github.com/dsmk-cpu"><img src="https://github.com/dsmk-cpu.png" width="60" style="border-radius:50%" alt="dsmk-cpu"/></a>
<a href="https://github.com/KSMEHMET42"><img src="https://github.com/KSMEHMET42.png" width="60" style="border-radius:50%" alt="KSMEHMET42"/></a>
<a href="https://github.com/halitcinar"><img src="https://github.com/halitcinar.png" width="60" style="border-radius:50%" alt="halitcinar"/></a>

---

<div align="center">
<sub>THM · Software Engineering: Realisierung · Gruppe 3 · SS26</sub>
</div>
