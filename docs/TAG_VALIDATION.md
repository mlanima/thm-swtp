# Tag Validation

New tags are validated against the StackExchange API before being persisted. Validation only runs when creating a tag that doesn't exist locally — once a tag is in the database, it's accepted without further checks. Results are cached in Redis (`redis.ser.mlanima.org`) with a 1-hour TTL.

The validation source is swappable via `app.tags.source` in `application.yaml`. The `TagSource` interface can be reimplemented (database-backed, GitHub Topics, static list) without modifying `ProjectTagService` or `ProfileTagService`.

---

## Architecture

```
TagSource (interface)
    │
    └── StackOverflowTagSource      # RestClient → StackExchange API v2.3
            │
            ▼
    TagValidationService            # @Cacheable — delegates to TagSource
            │
            ▼
    ProjectTagService / ProfileTagService   # calls isValidTag() on new tag only
```

The active implementation is selected by `@ConditionalOnProperty(name = "app.tags.source")` on `StackOverflowTagSource`.

---

## Data Flow

```
User submits "typescript"
  → ProjectTagService.addTagToProject()
    → getOrCreateTag("typescript")
      → tagRepository.findByNameIgnoreCase("typescript")
        → FOUND → return existing TagEntity (no API call)
        → NOT FOUND → tagValidationService.isValidTag("typescript")
          → @Cacheable("tag-exists")
            → REDIS HIT → return cached true/false
            → REDIS MISS → StackOverflowTagSource.tagExists("typescript")
              → GET /2.3/tags/typescript/info?site=stackoverflow
              → Cache result in Redis (TTL: 1 hour)
          → VALID → tagRepository.save(new TagEntity("typescript")) → 200
          → INVALID → throw TagNotValidException → 400
```

---

## Configuration

### application.yaml

```yaml
spring:
  cache:
    type: ${SPRING_CACHE_TYPE:redis}
    redis:
      time-to-live: 3600000           # 1 hour
      cache-null-values: false
      key-prefix: "tags:"
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis.ser.mlanima.org}
      port: 6379
      password: ${SPRING_DATA_REDIS_PASSWORD:}
      timeout: 2000ms
      connect-timeout: 1000ms

app:
  tags:
    source: stackoverflow

stackoverflow:
  api:
    base-url: https://api.stackexchange.com/2.3
    key: ${STACKOVERFLOW_API_KEY:}    # optional; 300 req/day without, 10k with
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_CACHE_TYPE` | No | `redis` | Set to `none` to disable Redis caching |
| `SPRING_DATA_REDIS_HOST` | No | `redis.ser.mlanima.org` | Redis server hostname |
| `SPRING_DATA_REDIS_PASSWORD` | In prod | empty | Redis `requirepass` value |
| `STACKOVERFLOW_API_KEY` | No | empty | StackExchange API app key |

Secrets are baked into the Docker image via build args in the CI/CD pipeline. No server-side compose changes needed.

---

## Error Handling

| Exception | HTTP | Log Level | Message |
|-----------|------|-----------|---------|
| `TagNotValidException` | 400 | debug | `"Tag 'xyz' is not a valid StackOverflow tag"` |
| `TagValidationException` | 502 | error | `"Tag validation service temporarily unavailable"` |

Frontend: both `TagList` and `ProfileTagListComponent` check `err.status === 400` and the `not a valid` substring in the response body, then show `PROJECTSITE.TAGS.ERROR_NOT_VALID` with the tag name.

---

## File Layout

```
api/src/main/java/de/thm/swtp/api/
├── config/CacheConfig.java               # @EnableCaching, RedisCacheManager
├── tag/
│   ├── exception/TagNotValidException.java
│   ├── service/ProjectTagService.java    # modified — injects TagValidationService
│   ├── service/ProfileTagService.java    # modified — injects TagValidationService
│   └── validation/
│       ├── TagSource.java                # interface
│       ├── StackOverflowTagSource.java
│       ├── TagValidationService.java     # @Cacheable wrapper
│       └── TagValidationException.java
└── exceptionhandling/GlobalExceptionHandler.java  # modified — handles both exceptions
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

---

## Local Development

The Redis instance (`redis.ser.mlanima.org`) is firewalled to `144.76.176.84/32` only — unreachable from local machines.

### Disable Caching (Recommended)

```bash
SPRING_CACHE_TYPE=none ./mvnw spring-boot:run
```

Every `isValidTag()` call goes directly to the StackExchange API. Acceptable for dev volumes.

### Local Redis

```bash
docker run -d -p 6379:6379 redis:7-alpine
SPRING_DATA_REDIS_HOST=localhost ./mvnw spring-boot:run
```

---

## CI/CD

1. Push to `developer`/`main` triggers `cd-build-deploy.yml`
2. Docker image built with `--build-arg SPRING_DATA_REDIS_PASSWORD` and `--build-arg STACKOVERFLOW_API_KEY`
3. Image pushed to GHCR, deployed via `deploy-app.bb` on `swtp-ss26.de`

---

## Future Improvements

- **`CacheErrorHandler`** — If Redis is unreachable, degrade to direct SO API calls instead of failing.
- **Respect StackExchange `backoff`** — Sleep N+1 seconds after receiving `backoff: N`. Currently only logged; caching makes repeated calls rare.
- **Cache warmup from SO data dump** — Stack Exchange publishes quarterly dumps (~100MB tags only) that can be imported to pre-populate the cache.
- **Bloom filter pre-check** — ~1MB in-memory filter of all valid tags. Eliminates API calls for invalid tags entirely.
- **StackExchange `filter` parameter** — Custom filters to reduce response payload (`?filter=!)`) and save bandwidth.
