---
title: Keycloak – Setup & Konfiguration
created: 2026-05-17 11:59
modified: 2026-05-17 12:38
---

# Keycloak – Setup & Konfiguration

## Voraussetzungen

- Docker + Docker Compose auf dem Server
- Reverse Proxy (hier: Traefik) läuft und das externe Netzwerk `traefik-net` existiert
- DNS-Eintrag für `auth.<domain>` zeigt auf den Server (A-Record oder Wildcard)
- `.env`-Datei befüllt (siehe unten)

---

## Stack-Übersicht

```
Keycloak 26.2  ←→  PostgreSQL 17
     ↑
  Traefik (TLS-Termination, Let's Encrypt)
```

Keycloak bekommt eine eigene PostgreSQL-Instanz im internen Netzwerk `keycloak_internal` — von außen nicht erreichbar. Traefik terminiert TLS und leitet HTTP-Traffic an Keycloak weiter.

---

## Verzeichnisstruktur

```
infra/keycloak/
├── docker-compose.yml
├── .env              # Admin-PWD für den ersten Login
├── .env.example      # Template
└── keycloak_db_data/ # wird automatisch angelegt
```

---

## Konfiguration

### `.env` anlegen

```bash
cp .env.example .env
```

`.env` befüllen:

```env
KC_ADMIN=admin
KC_ADMIN_PASSWORD=<stark...ort>
KC_DB_USER=keycloak
KC_DB_PASSWORD=<stark...ort>
```

### `docker-compose.yml`

Hostname in `KC_HOSTNAME` und Traefik-Label auf die eigene Domain anpassen:

```yaml
KC_HOSTNAME: auth.<domain>
traefik.http.routers.keycloak.rule=Host(`auth.<domain>`)
```

---

## Starten

```bash
cd infra/keycloak
docker compose up -d
```

Erster Start dauert ~60 Sekunden. Status prüfen:

```bash
docker compose logs -f keycloak
```

Bereit wenn `Keycloak 26.2 on JVM started` erscheint.

Admin-UI: `https://auth.<domain>` — mit den Credentials aus `.env` einloggen.

---

## Permanenten Admin-Account anlegen

Der Bootstrap-Admin (`KC_ADMIN`) ist ein temporärer Account und sollte ersetzt werden:

1. **Users → Create user**
   - Username: wählen
   - Email verified: **On**
2. **Credentials Tab → Set password** — Temporary: **Off**
3. **Role mapping → Assign role** → Filter: "Filter by realm roles" → `admin` zuweisen
4. Mit neuem Account einloggen
5. Alten Bootstrap-User unter **Users** löschen

---

## Realm einrichten

Keycloak arbeitet mit **Realms** — isolierte Mandanten mit eigenen Usern, Rollen und Clients. Jede Uni bekommt später einen eigenen Realm. Für Entwicklung: `swtp`.

### Realm anlegen

**Manage realms → Create realm**
- Realm name: `swtp`
- Enabled: On
- Create

---

## Clients anlegen

Ein **Client** ist eine App, die Keycloak zur Authentifizierung nutzt. Wir haben zwei.

### swtp-frontend (Angular)

**Clients → Create client**

| Feld | Wert |
|---|---|
| Client type | OpenID Connect |
| Client ID | `swtp-frontend` |
| Client authentication | **Off** (public client) |
| Authentication flow | Standard flow, **Direct access grants** (nur für Tests — in Produktion deaktivieren) |
| Valid redirect URIs | `http://localhost:4200/*` |
| Web origins | `http://localhost:4200` |

Für Produktion zusätzlich eintragen: `https://<frontend-domain>/*`

### swtp-backend (Spring Boot)

**Clients → Create client**

| Feld | Wert |
|---|---|
| Client type | OpenID Connect |
| Client ID | `swtp-backend` |
| Client authentication | **On** (confidential) |
| Authentication flow | Standard flow |
| Redirect URIs / Web origins | leer lassen |

---

## Rollen anlegen

**Realm roles → Create role** — jeweils anlegen:

- `USER` — Standardrolle für eingeloggte Nutzer
- `ADMIN` — erweiterte Rechte

Die Rollen sind zunächst nur Namen. Was sie im Backend bedeuten (welche Endpoints zugänglich sind), wird in der Spring-`SecurityConfig` definiert.

---

## Test-User anlegen

**Users → Create user**

| Feld | Wert |
|---|---|
| Username | `testuser` |
| Email verified | **On** |
| PWD | swtp26 |

Nach dem Speichern:
- **Credentials Tab** → Passwort setzen, Temporary: **Off**
- **Role mapping** → `USER` zuweisen

---

## Verifizierung

Token via curl holen:

```bash
curl -X POST \
  https://auth.<domain>/realms/swtp/protocol/openid-connect/token \
  -d "client_id=swtp-frontend" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=<passwort>"
```

Erfolgreiche Antwort enthält `access_token`, `refresh_token` und `expires_in`.

Den `access_token` kann man auf [jwt.io](https://jwt.io) dekodieren — unter `realm_access.roles` sollte `USER` erscheinen.

---

## Spring Boot Integration

`application.yaml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.<domain>/realms/swtp
```

Spring holt sich den Public Key automatisch von Keycloak und validiert damit eingehende JWTs. Weitere Details im KB-Artikel **Keycloak — Architektur & Integration**.

---

## Häufige Probleme

| Problem | Ursache | Fix |
|---|---|---|
| Keycloak startet nicht | DB noch nicht bereit | `depends_on` mit healthcheck löst das normalerweise — kurz warten |
| `invalid_grant: Account is not fully set up` | Passwort als Temporary gesetzt | Users → testuser → Credentials → Passwort neu setzen, Temporary: **Off** |
| `403` beim Token-Holen | User hat keine Rolle | Role mapping prüfen |
| CORS-Fehler im Browser | Origin nicht in Web origins | Client-Settings → Web origins ergänzen |
| `Invalid token` im Backend | Falsche `issuer-uri` | URL muss exakt mit `iss`-Claim im Token übereinstimmen |
| CrowdSec blockt Keycloak-Requests | Zu viele Fehlversuche | `cscli decisions delete --ip <ip>` auf dem Server |