# Tag-Validierung – Neues Feature

Implementiert die Tag-Validierung von Grund auf für `ProjectTagService` und `ProfileTagService`. Neue Tags werden vor dem Speichern geprüft; bereits in der DB vorhandene Tags werden ohne Validierung akzeptiert. Ergebnisse werden in Redis gecached (1h TTL) mit automatischem Fallback auf `NoOpCacheManager`, wenn Redis nicht erreichbar ist. Gecached werden nur ungültige Tags (`false`); gültige Tags (`true`) landen in der DB und werden dort gefunden.

## Architektur

```
TagSource (Interface) ← TagValidationService (@Cacheable) ← ProjectTagService / ProfileTagService
```

Drei austauschbare Implementierungen via `app.tags.source`:

| Modus | Beschreibung |
|---|---|
| `github-blocklist` (Standard) | LDNOOBW-Blockliste (28 Sprachen) → GitHub Topics API |
| `github` | Nur GitHub Topics API — keine Moderation |
| `stackoverflow` | StackExchange API v2.3 |

## Entwurfsentscheidungen

- **LDNOOBW-Wortliste** (~5.000 Einträge, 28 Sprachen) im Repository committed — keine hartcodierte Blockliste, community-gepflegt, kein Build-Time-Download nötig
- **GitHub Topics als Standard-API-Quelle** — kein API-Key erforderlich, 60 req/h ausreichend durch 1h Redis-Cache
- **`@ConditionalOnProperty`-Auswahl** — jede Quelle isoliert; Umschalten ist ein einzelner Konfigurationswert
- **Blockliste läuft vor dem Cache** — blockierte Begriffe erreichen weder Redis noch die externe API
- **`CacheManager` mit automatischem Fallback** — Ping bei Startup; bei Erfolg `RedisCacheManager`, bei Fehler `NoOpCacheManager` (kein Caching, kein Crash)
- **`CacheErrorHandler`** — fängt alle `RuntimeException` aus Cache-Operationen, nicht nur `RedisConnectionFailureException`
- **`unless = "#result"`** — gültige Tags (`true`) werden nicht gecached, da sie in der DB persistiert sind und dort gefunden werden; nur ungültige (`false`) landen im Cache
- **Secrets zur Laufzeit injiziert** — `STACKOVERFLOW_API_KEY` entfernt aus Docker-Build-Args und `ENV`, Injektion via Compose environment / `.env`-Datei

## Neue Dateien

| Datei | Zweck |
|---|---|
| `BlocklistService.java` | Lädt LDNOOBW-Wortliste beim Start |
| `GitHubTopicsClient.java` | Gemeinsam genutzter GitHub Topics API-Client |
| `GitHubTopicsTagSource.java` | Unmoderierte GitHub-Quelle |
| `ModeratedTagSource.java` | Standard — Blockliste + GitHub |
| `scripts/download-bad-words.*` | Manuelle Update-Tools |
| `src/main/resources/bad-words/*` | 28 LDNOOBW-Sprachdateien |

Siehe `docs/TAG_VALIDATION.md` für vollständige Details.
