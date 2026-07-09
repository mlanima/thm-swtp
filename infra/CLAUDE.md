# infra/

Deployment infrastructure for IdeaCamp on `swtp-ss26.de`.

## What lives here

### `infra/swtp-ss26.de/` — server stacks (review this)

Mirrors the directory layout on the server under `/opt/stacks/`. Three Docker Compose stacks:

- **`swtp-infra/`** — shared infrastructure: MySQL 9 (central DB for all envs), Keycloak (auth), Hoppscotch (REST client), status dashboard. Always running. Redis is included as a commented-out service — required for caching but currently served by an external instance.
- **`swtp-main/`** — production stack (API + frontend + Dozzle log viewer). Auto-deployed on push to `main` → image tag `latest`.
- **`swtp-dev/`** — developer stack (API + frontend + Dozzle). Auto-deployed on push to `developer` → image tag `dev`.

Everything routes through Traefik (runs separately on the host), which handles TLS via Let's Encrypt (INWX DNS challenge).

### Review apps (ephemeral, no compose file in repo)

Each PR gets a dynamically spun-up environment: `swtp_pr_<number>` DB schema (cloned from `swtp_template`), dedicated API/frontend/Dozzle containers, Traefik routes, and a Keycloak client config. Torn down when the PR closes. Orchestrated by the `.bb` scripts (see below).

### DB schemas on the server

| Schema | Purpose |
|---|---|
| `swtp_main` | Production data |
| `swtp_dev` | Developer branch data |
| `swtp_pr_<n>` | Per-PR review app (ephemeral) |
| `swtp_template` | Template schema cloned for each new review app |

### Babashka scripts (`swtp-infra/*.bb`) — review these

These run on the server, invoked via SSH or triggered by GitHub Actions:

- **`dispatch.bb`** — SSH forced-command router; maps `SSH_ORIGINAL_COMMAND` to the right deploy script.
- **`deploy.bb`** — starts the `swtp-infra` shared stack (MySQL, Keycloak, Hoppscotch).
- **`deploy-app.bb`** — pulls & restarts `swtp-main` or `swtp-dev`; updates the dashboard JSON.
- **`review-deploy.bb`** — spins up a PR environment: clones `swtp_template` DB, provisions uploads dir, starts containers, registers Keycloak client, updates dashboard.
- **`review-teardown.bb`** — tears down a PR environment (best-effort, each step independent).

### `infra/swtp-ss26.de/stacks/swtp-infra/dashboard/` — review this

Static dashboard served at `status.swtp-ss26.de`. `data.json` is written by the deploy scripts at runtime.

## Do NOT review

- **`infra/db/mysql.sql`** — personal reference snapshot of the DB schema. Not infrastructure, not deployed anywhere.
- **`infra/scripts/sync-world.sh`** — personal script for syncing the repo. Not part of the project.
