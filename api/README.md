# API

Spring Boot REST API, gesichert via Keycloak JWT.

## Voraussetzungen

- Java 25
- Keycloak läuft auf `https://auth.swtp-ss26.de`, Realm `swtp` angelegt

## Starten

```bash
./mvnw spring-boot:run
```

Läuft auf `http://localhost:8080`.

## Endpoints

| Endpoint | Auth | Beschreibung |
|---|---|---|
| `GET /api/public/hello` | — | Öffentlich |
| `GET /api/hello` | JWT required | Gibt Username, UUID und Rollen zurück |

## Konfiguration

`src/main/resources/application.yaml` — Keycloak `issuer-uri` und SQLite-Pfad bei Bedarf anpassen.
