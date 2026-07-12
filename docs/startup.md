# Lokale Entwicklungsumgebung

## Voraussetzungen

- Java 25
- Node.js 20+ (npm 11+)
- Docker (optional)
- Redis (lokal, Standard-Port 6379)

## Backend starten

```bash
cd api
./mvnw spring-boot:run
```

Lauft auf `http://localhost:8080` mit lokaler SQLite-Datenbank.
Keycloak wird unter `https://auth.swtp-ss26.de` erwartet.

## Frontend starten

```bash
cd web/ideacamp
npm install
npm start
```

Lauft auf `http://localhost:4200`, API-Aufrufe werden an Port 8080 weitergeleitet.

## Discord Bot

Fur Discord-Features muss ein Discord-App erstellt, ein Bot angelegt und der Bot lokal gestartet werden:

1. Discord Developer Portal: Neue Application erstellen
2. Bot anlegen und Token kopieren
3. `discord-bot/.env` anlegen (nach `.env.example`):

```
DISCORD_TOKEN=<dein-bot-token>
REDIS_HOST=localhost
REDIS_PORT=6379
PLATFORM_API_SECRET=<shared-secret>
PORT=3001
```

4. Bot starten:

```bash
cd discord-bot
npm install
npm run dev
```

## Umgebungsvariablen fur das Backend

Optional in `api/.env`:

```
SPRING_DATA_REDIS_HOST=localhost
DISCORD_BOT_BASEURL=http://localhost:3001
DISCORD_BOT_API_SECRET=<shared-secret>
DISCORD_CLIENT_ID=<deine-client-id>
DISCORD_CLIENT_SECRET=<dein-client-secret>
DISCORD_REDIRECT_URI=http://localhost:8080/api/v1/auth/discord/callback
```
