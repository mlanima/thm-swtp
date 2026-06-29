# StackOverflow Tag Validation

> Restricts tag creation to only tags that exist on StackOverflow. Uses the StackExchange API v2.3, Redis caching via `redis.ser.mlanima.org`, designed with SOLID + OOP so the source or validation strategy can be swapped later without modifying existing code.

---

## Table of Contents

1. [Motivation](#1-motivation)
2. [Architecture & SOLID Design](#2-architecture--solid-design)
3. [Data Flow](#3-data-flow)
4. [Caching Strategy](#4-caching-strategy)
5. [File Layout](#5-file-layout)
6. [Configuration Reference](#6-configuration-reference)
7. [Error Handling](#7-error-handling)
8. [Local Development](#8-local-development)
9. [Deployment Requirements](#9-deployment-requirements)
10. [Future Improvements](#10-future-improvements)

---

## 1. Motivation

Previously, any user could create arbitrary tags (free-text input). This led to:

- **Inconsistency** — "Java", "java", "JAVA" could all exist as separate tags
- **Spam / noise** — meaningless tags could be created
- **No alignment** with established tag ecosystems

By restricting tags to StackOverflow's tag set, we:

- Leverage a curated, community-maintained taxonomy (60k+ tags)
- Get built-in normalization (StackOverflow enforces lowercase)
- Future-proof the system — the `TagSource` interface allows swapping StackOverflow for any other source (GitHub Topics, a curated list, a local database) without touching business logic.

---

## 2. Architecture & SOLID Design

```
                    ┌──────────────────────┐
                    │  TagSource (interface) │  ← DIP: depend on abstraction
                    ├──────────────────────┤
                    │  + boolean            │
                    │    tagExists(name)    │
                    └──────────┬───────────┘
                               │ implements
                    ┌──────────▼───────────┐
                    │ StackOverflowTagSource│  ← SRP: external API only
                    │ (RestClient, backoff, │
                    │  quota_remaining)     │
                    └──────────┬───────────┘
                               │ used by
                    ┌──────────▼───────────┐
                    │ TagValidationService  │  ← SRP: caching + delegation
                    │ @Cacheable("tag-exists")│
                    └──────────┬───────────┘
                               │ injected into
                    ┌──────────▼───────────┐  ┌──────────────────────┐
                    │ ProjectTagService     │  │ ProfileTagService    │
                    │ getOrCreateTag()      │  │ getOrCreateTag()     │
                    └──────────────────────┘  └──────────────────────┘
```

### SOLID Justification

| Principle | How It's Met |
|-----------|-------------|
| **Single Responsibility** | `TagSource` handles external lookup; `TagValidationService` handles caching + delegation; `ProjectTagService` / `ProfileTagService` handle persistence. Each has one reason to change. |
| **Open/Closed** | New tag sources (file-based, GitHub API, database-backed) implement `TagSource`. No existing code is modified — only new implementations are added. |
| **Liskov Substitution** | Any `TagSource` implementation can replace another without breaking callers. The contract (`boolean tagExists(String)`) is narrow and well-defined. |
| **Interface Segregation** | `TagSource` has a single method — not polluted with unrelated concerns like tag search, creation, or deletion. |
| **Dependency Inversion** | `TagValidationService` depends on the `TagSource` **interface**, never on `StackOverflowTagSource` concretely. Wiring is done by Spring DI. |

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Interface + `@ConditionalOnProperty`** | The active source is chosen via `app.tags.source` in `application.yaml`. Future sources can be added with `@ConditionalOnProperty(name = "app.tags.source", havingValue = "...")` — no if/else chains. |
| **Validation in `getOrCreateTag()`** | Only validates when creating a **new** tag (not in the local DB). Existing tags skip the check entirely — once valid, always valid. This minimizes API calls. |
| **`@Cacheable` on service layer** | Spring's cache abstraction handles Redis read/write transparently. No manual cache put/get logic. |
| **StackExchange REST client** | `RestClient` (Spring Boot 4.x) instead of `RestTemplate` — the modern, non-blocking HTTP client with a fluent API. |

---

## 3. Data Flow

```
User submits "typescript" as a new tag
         │
         ▼
ProjectTagService.addTagToProject()
         │
         ▼
getOrCreateTag("typescript")
         │
         ├── tagRepository.findByNameIgnoreCase("typescript")
         │      │
         │      ├── FOUND → return existing TagEntity (no API call)
         │      │
         │      └── NOT FOUND → continue
         │
         ▼
tagValidationService.isValidTag("typescript")
         │
         ├── @Cacheable("tag-exists", key="typescript")
         │      │
         │      ├── REDIS HIT (key "tags::typescript" exists) → return cached true/false
         │      │
         │      └── REDIS MISS → continue
         │
         ▼
StackOverflowTagSource.tagExists("typescript")
         │
         ├── GET /2.3/tags/typescript/info?site=stackoverflow[&key=...]
         │      │
         │      ├── items not empty → exists ✓
         │      │
         │      └── items empty → does not exist ✗
         │
         ▼
Cache result in Redis (TTL: 1 hour)
         │
         ├── VALID → tagRepository.save(new TagEntity("typescript")) → return 200
         │
         └── INVALID → throw TagNotValidException → GlobalExceptionHandler → 400
```

---

## 4. Caching Strategy

### Redis Cache Configuration

| Setting | Value | Rationale |
|---------|-------|-----------|
| **Cache name** | `tag-exists` | Logical name for the cache region |
| **Key prefix** | `tags:` | Avoids collision with other cache regions in the same Redis instance |
| **TTL** | 1 hour | Balances freshness with API quota preservation. Tag sets change slowly on StackOverflow. |
| **Null values** | Disabled | No benefit to caching "does not exist" results longer than TTL (handled by `@Cacheable` default) |
| **Value serializer** | `StringRedisSerializer` | Caches `"true"` / `"false"` — simpler and faster than JSON serialization for a boolean |

### Cache Key Design

```
tags::typescript      → "true"   (exists)
tags::nonexistentxyz  → "false"  (does not exist)
```

Keys are **lowercased** (`#tagName.toLowerCase()`) so lookups are case-insensitive:
- `tagValidationService.isValidTag("TypeScript")` and `isValidTag("typescript")` hit the same cache entry.

### Why Redis Instead of Caffeine

| Factor | Redis | Caffeine (in-process) |
|--------|-------|----------------------|
| Shared across API instances | ✓ | ✗ (per-instance) |
| Survives restarts | ✓ (persisted) | ✗ |
| Single dependency | Needs a Redis server | Just a Maven dep |

For a single-instance deployment, Caffeine would work. Redis was chosen because the user explicitly preferred the distributed architecture, and the shared/public Redis instance via `redis.ser.mlanima.org` was already provisioned.

---

## 5. File Layout

```
api/src/main/java/de/thm/swtp/api/
├── config/
│   └── CacheConfig.java              # @EnableCaching, RedisCacheManager config
├── tag/
│   ├── exception/
│   │   └── TagNotValidException.java  # 400 — tag not found on StackOverflow
│   ├── service/
│   │   ├── ProjectTagService.java     # MODIFIED — injects TagValidationService
│   │   └── ProfileTagService.java     # MODIFIED — injects TagValidationService
│   └── validation/                    # NEW PACKAGE
│       ├── TagSource.java              # Interface — source of truth for valid tags
│       ├── StackOverflowTagSource.java # StackExchange API v2.3 implementation
│       ├── TagValidationService.java   # @Cacheable wrapper around TagSource
│       └── TagValidationException.java # 502 — StackOverflow API unreachable
├── exceptionhandling/
│   └── GlobalExceptionHandler.java     # MODIFIED — handles both new exceptions
```

### Adding a New Tag Source (e.g., a local database)

1. Implement `TagSource`:
   ```java
   @Component
   @ConditionalOnProperty(name = "app.tags.source", havingValue = "database")
   public class DatabaseTagSource implements TagSource {
       @Override
       public boolean tagExists(String tagName) { ... }
   }
   ```
2. Set `app.tags.source=database` in `application.yaml`.
3. No other code changes — `TagValidationService`, `ProjectTagService`, `ProfileTagService` are all untouched.

---

## 6. Configuration Reference

### `application.yaml`

```yaml
spring:
  cache:
    type: ${SPRING_CACHE_TYPE:redis}       # "redis" or "none" (disable for local dev)
    redis:
      time-to-live: 3600000                # 1 hour
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
    source: stackoverflow                  # selects which TagSource to activate

stackoverflow:
  api:
    base-url: https://api.stackexchange.com/2.3
    key: ${STACKOVERFLOW_API_KEY:}         # optional; 300/day without, 10k/day with
```

### Environment Variables (GitHub Secrets → Docker Build Args)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_CACHE_TYPE` | No | `redis` | Set to `none` to disable Redis caching |
| `SPRING_DATA_REDIS_HOST` | No | `redis.ser.mlanima.org` | Redis server hostname |
| `SPRING_DATA_REDIS_PASSWORD` | In production | empty | Redis `requirepass` value |
| `STACKOVERFLOW_API_KEY` | No (recommended) | empty | StackExchange API app key |

### How Secrets Reach the Container

```
GitHub Secrets → cd-build-deploy.yml (build-args) → Dockerfile (ARG + ENV) → Spring Boot (application.yaml)
```

The password and API key are baked into the Docker image at build time via `docker/build-push-action@v7` build args. No server-side compose file modifications are needed. The image is private in GHCR.

---

## 7. Error Handling

### Exception Hierarchy

```
RuntimeException
├── TagNotValidException    → 400 BAD_REQUEST
│   (tag name is not a valid StackOverflow tag)
│
└── TagValidationException  → 502 BAD_GATEWAY
    (StackOverflow API is unreachable or returned an error)
```

### GlobalExceptionHandler Mapping

| Exception | HTTP Status | Log Level | Message |
|-----------|-------------|-----------|---------|
| `TagNotValidException` | 400 | `debug` | `"Tag 'xyz' is not a valid StackOverflow tag"` |
| `TagValidationException` | 502 | `error` | `"Tag validation service temporarily unavailable."` |

### Client Response Body

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Tag 'xyz123' is not a valid StackOverflow tag",
  "timestamp": "..."
}
```

### Frontend Handling

In `TagList` and `ProfileTagListComponent`, the `saveTag()` error callback checks for the `not a valid` substring in the response message and shows the specific i18n key `PROJECTSITE.TAGS.ERROR_NOT_VALID` with the tag name interpolated. All other errors fall back to the generic `ERROR_TOO_LONG` message.

---

## 8. Local Development

The default Redis host is `redis.ser.mlanima.org` (the EC2 instance), which is firewalled to only `144.76.176.84/32` (the production server). Developers cannot reach it from their local machines.

### Option A: Disable Caching (Simplest)

```bash
SPRING_CACHE_TYPE=none ./mvnw spring-boot:run
```

Every `isValidTag()` call hits StackOverflow directly — no Redis. Acceptable for dev volumes (300 requests/day without API key, 10k with).

### Option B: Run Redis Locally

```bash
docker run -d -p 6379:6379 redis:7-alpine
export SPRING_DATA_REDIS_HOST=localhost
./mvnw spring-boot:run
```

### Option C: IntelliJ / IDE

Set environment variable `SPRING_CACHE_TYPE=none` in the run configuration before starting.

---

## 9. Deployment Requirements

### External Dependencies

| Service | URL | Purpose | Required |
|---------|-----|---------|----------|
| Redis | `redis.ser.mlanima.org:6379` | Tag existence cache | Yes (falls back to no-cache if `SPRING_CACHE_TYPE=none`) |
| StackExchange API | `api.stackexchange.com:443` | Tag validation | Yes (fallback on network error → 502) |

### CI/CD Pipeline Flow

1. Push to `developer` or `main` triggers `.github/workflows/cd-build-deploy.yml`
2. `dorny/paths-filter` detects changes under `api/**`
3. `docker/login-action` authenticates to `ghcr.io`
4. `docker/build-push-action` builds the API image with:
   - `--build-arg SPRING_DATA_REDIS_PASSWORD=${{ secrets.SPRING_DATA_REDIS_PASSWORD }}`
   - `--build-arg STACKOVERFLOW_API_KEY=${{ secrets.STACKOVERFLOW_API_KEY }}`
5. Image is pushed to `ghcr.io/<org>/swtp-api:dev` (or `:latest` for main)
6. SSH to `swtp-ss26.de` → `deploy-app.bb` → `docker compose up -d` pulls and restarts

**No server-side compose file changes** are needed — the Redis host is baked into `application.yaml` as `redis.ser.mlanima.org` and resolves publicly over DNS.

---

## 10. Future Improvements

| Improvement | Effort | Benefit |
|-------------|--------|---------|
| **Pre-populate cache from data dump** | Medium | Zero API calls even on first use. Stack Exchange publishes quarterly data dumps (~100MB tags only) that can be imported once. |
| **Respect `backoff`** | Low | After receiving `backoff: N`, sleep N+1 seconds before next API call. Currently only logged. Acceptable because caching makes repeated calls very rare. |
| **Bloom filter pre-check** | Low | ~1MB in-memory Bloom filter of all valid tags. Sub-µs "definitely not valid" check before hitting Redis/API. Eliminates 100% of API calls for invalid tags. |
| **Scheduled cache warmup** | Medium | `@Scheduled` job that refreshes popular tags from SO API nightly during low-traffic hours. |
| **`CacheErrorHandler`** | Low | If Redis is unreachable, degrade gracefully (call SO API directly instead of failing). Currently `@Cacheable` throws if Redis is down. |
| **Multi-tier (Caffeine + Redis)** | Medium | Add Caffeine L1 cache for sub-ms hits on the same instance. Redis L2 for cross-instance sharing. |
| **StackExchange API `filter` parameter** | Low | Use custom filters to reduce response payload (`?filter=!)` — saves bandwidth and quota. |

---

## Appendix: Audit of Logging Conventions

All logging follows `api/CLAUDE.md` conventions:

| Location | Level | Rationale |
|----------|-------|-----------|
| `StackOverflowTagSource` — API request URI | `debug` | Query params / trace — off in prod |
| `StackOverflowTagSource` — null response | `error` | Unexpected IO/technical failure |
| `StackOverflowTagSource` — backoff received | `warn` | Transient API throttle signal |
| `StackOverflowTagSource` — quota exhausted | `warn` | Operator attention needed |
| `StackOverflowTagSource` — existence result | `debug` | Hit count — off in prod |
| `TagValidationService` — cache miss | `debug` | Search/hit ratio — off in prod |
| `TagValidationService` — cache hit | silent | Reads stay silent unless answering an incident |
| `GlobalExceptionHandler` — `TagNotValidException` | `debug` | 400 → debug per convention |
| `GlobalExceptionHandler` — `TagValidationException` | `error` | 5xx → error per convention |
| `ProjectTagService` — tag added | `info` | Lifecycle event with resource id |
| `ProfileTagService` — tag added | `info` | Lifecycle event with resource id |
