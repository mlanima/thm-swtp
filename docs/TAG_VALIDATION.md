# Tag Validation

New tags are validated before being persisted. Validation only runs when creating a tag that doesn't exist locally — once a tag is in the database, it's accepted without further checks. Results are cached in Redis (`redis.ser.mlanima.org`) with a 1-hour TTL. Only invalid tags (`false`) are cached — valid tags are persisted in the DB and found there on subsequent lookups. If Redis is unreachable at startup, the `CacheManager` falls back to `NoOpCacheManager` (no caching, full functionality preserved).

Three validation modes are available via `app.tags.source` in `application.yaml`. The `TagSource` interface can be further reimplemented without modifying `ProjectTagService` or `ProfileTagService`.

---

## Architecture

```
TagSource (interface)
    │
    ├── ModeratedTagSource (default)         # BlocklistService + GitHubTopicsClient
    │       │
    │       ├── BlocklistService             # LDNOOBW wordlist (28 languages)
    │       └── GitHubTopicsClient           # RestClient → GitHub Search Topics API
    │
    ├── GitHubTopicsTagSource                # RestClient → GitHub Search Topics API (no blocklist)
    │
    └── StackOverflowTagSource               # RestClient → StackExchange API v2.3
            │
            ▼
    TagValidationService            # @Cacheable — delegates to TagSource
            │
            ▼
    ProjectTagService / ProfileTagService   # calls isValidTag() on new tag only
```

Active implementation selected by `@ConditionalOnProperty(name = "app.tags.source")`.

---

## Validation Modes

| `app.tags.source` | Description |
|---|---|
| `github-blocklist` (default) | Checks LDNOOBW blocklist first, then queries GitHub Topics API |
| `github` | Queries GitHub Topics API directly — no moderation |
| `stackoverflow` | Queries StackExchange API v2.3 — implicitly clean (community-moderated) |

---

## Data Flow (github-blocklist mode)

```
User submits "typescript"
  → ProjectTagService.addTagToProject()
    → getOrCreateTag("typescript")
      → tagRepository.findByNameIgnoreCase("typescript")
        → FOUND → return existing TagEntity (no API call)
        → NOT FOUND → tagValidationService.isValidTag("typescript")
          → @Cacheable(value = "tag-exists", unless = "#result")
            → REDIS HIT → return cached false (invalid tag)
            → REDIS MISS → ModeratedTagSource.tagExists("typescript")
              → BlocklistService.contains("typescript")?  no
              → GitHubTopicsClient.tagExists("typescript")
                → GET /search/topics?q=typescript
              → valid → result is NOT cached (unless = "#result")
          → VALID → tagRepository.save(new TagEntity("typescript")) → 200
          → INVALID → throw TagNotValidException → 400

User submits "fuck"
  → ...tagValidationService.isValidTag("fuck")
    → @Cacheable("tag-exists")
      → REDIS HIT → return false (cached from previous)
      → REDIS MISS → ModeratedTagSource.tagExists("fuck")
        → BlocklistService.contains("fuck")?  yes → return false
        → Cache result in Redis (TTL: 1 hour)
    → INVALID → throw TagNotValidException → 400
```

Only invalid tags get cached (as `false`) to avoid re-checking the blocklist or external API on every attempt. Valid tags are not cached — they are persisted in the DB and found via `findByNameIgnoreCase` on subsequent lookups.

---

## Configuration

### application.yaml

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis.ser.mlanima.org}
      port: 6379
      timeout: 2000ms
      connect-timeout: 1000ms

app:
  tags:
    source: github-blocklist          # "github-blocklist" (default), "github", or "stackoverflow"

github:
  topics:
    api:
      base-url: https://api.github.com

stackoverflow:
  api:
    base-url: https://api.stackexchange.com/2.3
    key: ${STACKOVERFLOW_API_KEY:}    # optional; 300 req/day without, 10k with
```

### Switching Modes

| Mode | Config Change |
|---|---|
| Blocklist + GitHub (default) | `app.tags.source: github-blocklist` |
| GitHub Topics (no moderation) | `app.tags.source: github` |
| StackOverflow | `app.tags.source: stackoverflow` |

The `@ConditionalOnProperty` on each implementation ensures only the matching source is active.

### Blocklist Updates

Bad-word files are committed to the repository. To refresh them, run the platform-specific script from the `api/` directory:

```bash
# Linux/macOS
bash scripts/download-bad-words.sh

# Windows
.\scripts\download-bad-words.ps1
```

Supported languages (28): ar, cs, da, de, en, eo, es, fa, fi, fil, fr, fr-CA-u-sd-caqc, hi, hu, it, ja, kab, ko, nl, no, pl, pt, ru, sv, th, tlh, tr, zh.

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_DATA_REDIS_HOST` | No | `redis.ser.mlanima.org` | Redis server hostname |
| `STACKOVERFLOW_API_KEY` | If using SO | empty | StackExchange API app key — inject at runtime via Compose environment, **not** as Docker build-arg |

---

## Error Handling

| Exception | HTTP | Log Level | Message |
|-----------|------|-----------|---------|
| `TagNotValidException` | 400 | debug | `"Tag 'xyz' is not a valid tag"` |
| `TagValidationException` | 502 | error | `"Tag validation service temporarily unavailable"` |

Frontend: both `TagList` and `ProfileTagListComponent` check `err.status === 400` and the `not a valid` substring in the response body, then show `PROJECTSITE.TAGS.ERROR_NOT_VALID` with the tag name.

---

## File Layout

```
api/src/main/java/de/thm/swtp/api/
├── config/CacheConfig.java               # @EnableCaching, CacheManager (Redis/NoOp), CacheErrorHandler
├── tag/
│   ├── exception/TagNotValidException.java
│   ├── service/ProjectTagService.java    # injects TagValidationService
│   ├── service/ProfileTagService.java    # injects TagValidationService
│   └── validation/
│       ├── TagSource.java                # interface
│       ├── BlocklistService.java         # loads LDNOOBW wordlist at startup
│       ├── GitHubTopicsClient.java       # shared GitHub API client
│       ├── GitHubTopicsTagSource.java    # GitHub-only (no blocklist)
│       ├── ModeratedTagSource.java       # default — blocklist + GitHub
│       ├── StackOverflowTagSource.java   # alternative — StackExchange API
│       ├── TagValidationService.java     # @Cacheable wrapper
│       └── TagValidationException.java
├── scripts/
│   ├── download-bad-words.sh             # Linux/macOS
│   └── download-bad-words.ps1            # Windows
└── exceptionhandling/GlobalExceptionHandler.java  # handles both exceptions
```

### Adding a New Source

```java
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "database")
public class DatabaseTagSource implements TagSource {
    @Override
    public boolean tagExists(String tagName) { ... }
}
```

Set `app.tags.source=database` in `application.yaml`. No other code changes.

### Adding Blocklist-Only Validation

To reject blocked tags without checking any external API, create a tag source that only checks `BlocklistService`:

```java
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "blocklist-only")
public class BlocklistOnlyTagSource implements TagSource {
    private final BlocklistService blocklistService;
    // tagExists() returns true only if tag is NOT in the blocklist
}
```

---

## Local Development

Redis connections fail gracefully — if Redis is unreachable at startup, the `CacheManager` falls back to `NoOpCacheManager` (no caching, tag source called directly for every new tag). A warning is logged.

### No Redis (Works Out of the Box)

No configuration needed. The default Redis host is firewalled, so the `CacheManager` fallback kicks in automatically.

### Local Redis

```bash
docker run -d -p 6379:6379 redis:7-alpine
SPRING_DATA_REDIS_HOST=localhost ./mvnw spring-boot:run
```

---

## CI/CD

1. Push to `developer`/`main` triggers `cd-build-deploy.yml`
2. Docker image built without secrets; `STACKOVERFLOW_API_KEY` is injected at runtime via Docker Compose environment or `.env` file
3. Image pushed to GHCR, deployed via `deploy-app.bb` on `swtp-ss26.de`

GitHub Topics API requires no authentication (60 req/hr unauthenticated, sufficient with 1h cache).

---

## Future Improvements

- **GitHub token for higher rate limit** — Pass a `GITHUB_TOKEN` to get 5,000 req/hr instead of 60.
- **Bloom filter pre-check** — ~1MB in-memory filter of all known tags. Eliminates API calls for invalid tags entirely.
- **Cache warmup** — Pre-populate the Redis cache with commonly used tags on startup.
