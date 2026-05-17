# API

Spring Boot REST API für IdeaCamp.

## Voraussetzungen

- Java 25
- Maven (oder `./mvnw`)

## Starten

```bash
./mvnw spring-boot:run
```

Läuft momentan mit 'ner lokalen SQLite (nicht dauerhaft) auf `http://localhost:8080`.

## Testen

```bash
curl http://localhost:8080
```

Erwartete Antwort: `401 Unauthorized` — Security ist aktiv, API läuft.
