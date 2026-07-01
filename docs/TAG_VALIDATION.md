# Tag Validation

New tags are validated before being persisted. Validation only runs when creating a tag that doesn't exist locally — once a tag is in the database, it's accepted without further checks. Results are cached in Redis (`redis.ser.mlanima.org`) with a 1-hour TTL. Only invalid tags (`false`) are cached — valid tags are persisted in the DB and found there on subsequent lookups. If Redis is unreachable at startup, the `CacheManager` falls back to `NoOpCacheManager` (no caching, full functionality preserved).

Four validation modes are available via `app.tags.source` in `application.yaml`. The `TagSource` interface can be further reimplemented without modifying `ProjectTagService` or `ProfileTagService`.

---

## Architecture

```
TagSource (interface)
    │
    ├── OpenAIModeratedGithubTagSource (default) # OpenAI moderation + GitHub Topics
    │       │                                    # BlocklistService as fallback
    │       ├── OpenAIModerationClient           # RestClient → OpenAI Moderation API
    │       ├── BlocklistService                 # LDNOOBW wordlist (fallback)
    │       └── GitHubTopicsClient               # RestClient → GitHub Search Topics API
    │
    ├── ModeratedTagSource               # BlocklistService + GitHubTopicsClient
    │       │
    │       ├── BlocklistService         # LDNOOBW wordlist (28 languages)
    │       └── GitHubTopicsClient       # RestClient → GitHub Search Topics API
    │
    ├── GitHubTopicsTagSource            # RestClient → GitHub Search Topics API (no blocklist)
    │
    └── StackOverflowTagSource           # RestClient → StackExchange API v2.3
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
| `openai-github` (default) | OpenAI Omni Moderation first (falls back to LDNOOBW blocklist on API failure), then GitHub Topics API |
| `github-blocklist` | Checks LDNOOBW blocklist first, then queries GitHub Topics API |
| `github` | Queries GitHub Topics API directly — no moderation |
| `stackoverflow` | Queries StackExchange API v2.3 — implicitly clean (community-moderated) |

---

## Data Flow (openai-github mode — default)

```
User submits "typescript"
  → ProjectTagService.addTagToProject()
    → tagRepository.findByNameIgnoreCase("typescript")
      → NOT FOUND → tagValidationService.isValidTag("typescript")
        → @Cacheable(value = "tag-exists", unless = "#result")
          → REDIS MISS → OpenAIModeratedGithubTagSource.tagExists("typescript")
            → OpenAIModerationClient.isFlagged("typescript")?  no
            → GitHubTopicsClient.tagExists("typescript")
              → GET /search/topics?q=typescript
            → valid → result is NOT cached (unless = "#result")
        → VALID → tagRepository.save(new TagEntity("typescript")) → 200

User submits "fuck"
  → ...tagValidationService.isValidTag("fuck")
    → OpenAIModeratedGithubTagSource.tagExists("fuck")
      → OpenAIModerationClient.isFlagged("fuck")?  yes → return false
    → INVALID → throw TagNotValidException → 400

User submits "fuck" while OpenAI is down
  → OpenAIModeratedGithubTagSource.tagExists("fuck")
    → OpenAIModerationClient.isFlagged("fuck")?
      → TagValidationException (API down)
    → BlocklistService.contains("fuck")?  yes (fallback) → return false
  → INVALID → throw TagNotValidException → 400

User submits "4gotjwp"
  → OpenAIModeratedGithubTagSource.tagExists("4gotjwp")
    → OpenAIModerationClient.isFlagged("4gotjwp")?  no (gibberish passes)
    → GitHubTopicsClient.tagExists("4gotjwp")?      no
  → INVALID → throw TagNotValidException → 400
```

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
    source: openai-github             # "openai-github" (default), "github-blocklist", "github", or "stackoverflow"

openai:
  api:
    base-url: https://api.openai.com
    key: ${OPENAI_API_KEY:}           # required for openai-github mode; set via GitHub Secret in CI
  moderation:
    model: omni-moderation-latest
    threshold: 0.1                    # category scores above this are flagged

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
|---|---|---|
| OpenAI + GitHub (default) | `app.tags.source: openai-github` |
| Blocklist + GitHub (legacy) | `app.tags.source: github-blocklist` |
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
| `OPENAI_API_KEY` | If using openai-github | empty | OpenAI API key — set as GitHub Secret, injected via deploy pipeline into `.env` |
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
│       ├── OpenAIModerationClient.java   # RestClient → OpenAI Moderation API (omni-moderation-latest)
│       ├── OpenAIModeratedGithubTagSource.java  # default — OpenAI + GitHub, blocklist fallback
│       ├── BlocklistService.java         # loads LDNOOBW wordlist at startup
│       ├── GitHubTopicsClient.java       # shared GitHub API client
│       ├── GitHubTopicsTagSource.java    # GitHub-only (no blocklist)
│       ├── ModeratedTagSource.java       # legacy — blocklist + GitHub
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

Or uncomment the `redis` service in `infra/swtp-ss26.de/stacks/swtp-infra/docker-compose.yml`
to run it as part of the shared infra stack.

---

## CI/CD

1. Push to `developer`/`main` triggers `cd-build-deploy.yml`
2. Docker image built without secrets; secrets are injected at runtime:
   - `STACKOVERFLOW_API_KEY` via Docker Compose environment or `.env` file
   - `OPENAI_API_KEY` passed as GitHub Secret → SSH command arg → `deploy-app.bb` writes to `.env`
3. Image pushed to GHCR, deployed via `deploy-app.bb` on `swtp-ss26.de`

GitHub Topics API requires no authentication (60 req/hr unauthenticated, sufficient with 1h cache).

OpenAI Moderation API is free to use with rate limits depending on usage tier (Free: 250 RPM / 5,000 RPD).

---

## Future Improvements

- **GitHub token for higher rate limit** — Pass a `GITHUB_TOKEN` to get 5,000 req/hr instead of 60.
- **Bloom filter pre-check** — ~1MB in-memory filter of all known tags. Eliminates API calls for invalid tags entirely.
- **Cache warmup** — Pre-populate the Redis cache with commonly used tags on startup.
