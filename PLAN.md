# PLAN — StackOverflow Tag Validation with Redis Caching

> Restrict tag creation to only tags that exist on StackOverflow. Uses the StackExchange API v2.3, Redis caching via `redis.ser.mlanima.org`, designed with SOLID/OOP so the source or validation strategy can be swapped later.

---

## 1. Data Flow

```
User adds tag → Lookup in local DB (exists? → return)
             → Miss? Check Redis cache (hit? → return valid)
             → Miss? Call StackOverflow API → store in Redis → return
             → Not valid? Throw TagNotValidException → 400 to client
```

---

## 2. Architecture — SOLID & OOP

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
                    │  quota_remaining log) │
                    └──────────┬───────────┘
                               │ used by
                    ┌──────────▼───────────┐
                    │ TagValidationService  │  ← SRP: caching + delegation
                    │ @Cacheable("tag-exists")│
                    └──────────┬───────────┘
                               │ injected into (OCP)
                    ┌──────────▼───────────┐  ┌──────────────────────┐
                    │ ProjectTagService     │  │ ProfileTagService    │
                    │ getOrCreateTag()      │  │ getOrCreateTag()     │
                    └──────────────────────┘  └──────────────────────┘
```

### SOLID Justification

| Principle | How It's Met |
|-----------|-------------|
| **SRP** | `TagSource` — external API; `TagValidationService` — caching; `ProjectTagService` / `ProfileTagService` — persistence |
| **OCP** | New sources (local DB, static list, GitHub API) implement `TagSource` without modifying existing code |
| **LSP** | Any `TagSource` can substitute the interface without breaking callers |
| **ISP** | `TagSource` has one focused method; not polluted with unrelated concerns |
| **DIP** | All services depend on the `TagSource` interface, not on `StackOverflowTagSource` |

---

## 3. File-by-File Implementation

### 3.1 Backend — New Files

#### 3.1.1 `api/src/main/java/de/thm/swtp/api/config/CacheConfig.java`

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer() {
        return builder -> builder
            .withCacheConfiguration("tag-exists",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .prefixWith("tags:")
                    .disableCachingNullValues()
                    .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            new StringRedisSerializer()))
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            new StringRedisSerializer())));
    }
}
```

- `@EnableCaching` activates Spring's cache abstraction
- 1-hour TTL balances freshness with API quota preservation
- `tags:` prefix avoids key collisions
- `StringRedisSerializer` for values (stores `"true"`/`"false"`) — simpler and faster than JSON for a boolean

#### 3.1.2 `api/src/main/java/de/thm/swtp/api/tag/validation/TagSource.java`

```java
@FunctionalInterface
public interface TagSource {
    boolean tagExists(String tagName);
}
```

- Single method — enabling lambda or composition-based implementations
- Pure abstraction with no domain dependency

#### 3.1.3 `api/src/main/java/de/thm/swtp/api/tag/validation/StackOverflowTagSource.java`

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "app.tags.source", havingValue = "stackoverflow", matchIfMissing = true)
public class StackOverflowTagSource implements TagSource {

    private static final String TAG_INFO_URL = "/2.3/tags/{tags}/info?site=stackoverflow";
    private static final int QUOTA_WARN_THRESHOLD = 100;

    private final RestClient restClient;

    public StackOverflowTagSource(
            @Value("${stackoverflow.api.base-url:https://api.stackexchange.com/2.3}") String baseUrl,
            @Value("${stackoverflow.api.key:}") Optional<String> apiKey) {

        var builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    log.debug("StackOverflow API request: {}", request.getURI());
                    return execution.execute(request, body);
                });

        this.restClient = builder.build();
    }

    @Override
    public boolean tagExists(String tagName) {
        var response = restClient.get()
                .uri(uriBuilder -> {
                    var uri = uriBuilder.path(TAG_INFO_URL);
                    apiKey.ifPresent(key -> uri.queryParam("key", key));
                    return uri.build(tagName);
                })
                .retrieve()
                .body(StackOverflowResponse.class);

        if (response == null) {
            log.warn("StackOverflow API returned null response for tag: {}", LogSafe.clean(tagName));
            throw new TagValidationException("StackOverflow API returned empty response");
        }

        if (response.backoff() > 0) {
            log.warn("StackOverflow API requested backoff of {}s for tag: {}",
                    response.backoff(), LogSafe.clean(tagName));
        }

        if (response.quotaRemaining() < QUOTA_WARN_THRESHOLD) {
            log.warn("StackOverflow API quota nearly exhausted: {}/{} remaining",
                    response.quotaRemaining(), response.quotaMax());
        }

        boolean exists = response.items() != null && !response.items().isEmpty();
        log.debug("Tag '{}' exists on StackOverflow: {}", LogSafe.clean(tagName), exists);
        return exists;
    }

    private record StackOverflowResponse(
            @JsonProperty("items") List<StackOverflowItem> items,
            @JsonProperty("backoff") int backoff,
            @JsonProperty("quota_max") int quotaMax,
            @JsonProperty("quota_remaining") int quotaRemaining) {}

    private record StackOverflowItem(
            @JsonProperty("name") String name,
            @JsonProperty("count") int count) {}
}
```

- `@ConditionalOnProperty` — source is swappable via config
- `RestClient` — Spring's modern HTTP client
- Response records are private — encapsulated within the class
- `backoff` logged; `@Cacheable` prevents duplicate calls anyway
- `quotaRemaining` warns at threshold so operator can add API key
- Fail-closed: throws on API error

#### 3.1.4 `api/src/main/java/de/thm/swtp/api/tag/validation/TagValidationService.java`

```java
@Slf4j
@Service
public class TagValidationService {

    private final TagSource tagSource;

    public TagValidationService(TagSource tagSource) {
        this.tagSource = tagSource;
    }

    @Cacheable(value = "tag-exists", key = "#tagName.toLowerCase()")
    public boolean isValidTag(String tagName) {
        log.info("Cache miss for tag '{}' — querying source", LogSafe.clean(tagName));
        return tagSource.tagExists(tagName);
    }
}
```

- Single `@Cacheable` annotation handles all Redis caching
- `#tagName.toLowerCase()` ensures case-insensitive cache keys
- Constructor injection for explicitness

#### 3.1.5 `api/src/main/java/de/thm/swtp/api/tag/exception/TagNotValidException.java`

```java
public class TagNotValidException extends RuntimeException {

    public TagNotValidException(String tagName) {
        super("Tag '" + LogSafe.clean(tagName) + "' is not a valid StackOverflow tag");
    }
}
```

#### 3.1.6 `api/src/main/java/de/thm/swtp/api/tag/validation/TagValidationException.java`

```java
public class TagValidationException extends RuntimeException {
    public TagValidationException(String message) {
        super(message);
    }
}
```

- For external API errors (network down, null response)
- Maps to 502

#### 3.2.6 `Dockerfile`

Add build args at the start of stage 3 so Spring Boot receives the secrets via environment variables:

```dockerfile
# Stage 3: Runtime
FROM eclipse-temurin:25-jre-alpine
ARG SPRING_DATA_REDIS_PASSWORD=
ENV SPRING_DATA_REDIS_PASSWORD=$SPRING_DATA_REDIS_PASSWORD
ARG STACKOVERFLOW_API_KEY=
ENV STACKOVERFLOW_API_KEY=$STACKOVERFLOW_API_KEY
```

**Why:** No SSH access to `swtp-ss26.de` — secrets are baked into the image at CI/CD build time via Docker build args, not via server compose files.

### 3.2 Backend — Modified Files

#### 3.2.1 `ProjectTagService.java`

Inject `TagValidationService` and guard `getOrCreateTag()`:

```java
private final TagValidationService tagValidationService;

private TagEntity getOrCreateTag(String tagName) {
    String cleaned = tagName.trim();

    return tagRepository.findByNameIgnoreCase(cleaned)
            .orElseGet(() -> {
                if (!tagValidationService.isValidTag(cleaned)) {
                    throw new TagNotValidException(cleaned);
                }
                return tagRepository.save(new TagEntity(cleaned));
            });
}
```

**Logic:** Look up local DB → if exists, return (no API call) → if not, validate → if invalid, throw 400 → if valid, save.

#### 3.2.2 `ProfileTagService.java`

Same change as `ProjectTagService` — identical `getOrCreateTag()` guard.

#### 3.2.3 `GlobalExceptionHandler.java`

Add two handlers:

```java
@ExceptionHandler(TagNotValidException.class)
public ResponseEntity<ErrorResponse> handleTagNotValid(TagNotValidException ex) {
    log.debug("Bad Request (400): {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
}

@ExceptionHandler(TagValidationException.class)
public ResponseEntity<ErrorResponse> handleTagValidationError(TagValidationException ex) {
    log.error("Tag validation failed due to external API error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of(502, "Bad Gateway", "Tag validation service temporarily unavailable."));
}
```

- `TagNotValidException` → 400 (user error)
- `TagValidationException` → 502 (external dependency failure)
- Log levels follow project conventions

#### 3.2.4 `application.yaml`

Add:

```yaml
spring:
  cache:
    type: ${SPRING_CACHE_TYPE:redis}
    redis:
      time-to-live: 3600000
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
    key: ${STACKOVERFLOW_API_KEY:}
```

**Default host** is `redis.ser.mlanima.org` — the CI/CD image doesn't need any env var override since it resolves over public DNS. **For local development** (no access to the Redis server), set `SPRING_CACHE_TYPE=none` to disable caching — the StackOverflow API is called every time, which is fine for dev volumes.

#### 3.2.5 `pom.xml`

Add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 3.3 Frontend — Modified Files

#### 3.3.1 `tag-list.ts`

Change `error` callback in `saveTag()`:

```typescript
error: (err) => {
  const apiError = err.error as { message?: string };
  if (apiError?.message?.includes('not a valid')) {
    this.errorMessage.set(
      this.translateService.instant('PROJECTSITE.TAGS.ERROR_NOT_VALID', { name: cleanedName })
    );
  } else {
    this.errorMessage.set(
      this.translateService.instant('PROJECTSITE.TAGS.ERROR_TOO_LONG')
    );
  }
  this.isSaving.set(false);
},
```

#### 3.3.2 `profile-tag-list.component.ts`

Same error handling change as `tag-list.ts`.

#### 3.3.3 `en.json` — under `PROJECTSITE.TAGS`

```json
"ERROR_NOT_VALID": "Tag '{{name}}' is not a valid StackOverflow tag."
```

#### 3.3.4 `de.json`

```json
"ERROR_NOT_VALID": "Tag '{{name}}' ist kein gültiger StackOverflow-Tag."
```

---

## 4. Infrastructure — Server Setup

### 4.1 EC2 (already done by you)

| Step | Status |
|------|--------|
| DNS `redis.ser.mlanima.org` → EC2 IP | Done |
| `apt install redis` | Done |
| `bind 0.0.0.0`, `requirepass`, `maxmemory 128mb` | Done |
| AWS Security Group: `144.76.176.84/32` → TCP 6379 | Done |

### 4.2 No Server Changes Needed

**No SSH, no Docker Compose edits, no `.env` files.** All configuration reaches the container via Docker build args baked into the image at CI/CD time.

---

## 6. CI/CD

### `.github/workflows/cd-build-deploy.yml`

Add `build-args` to the `build-backend` job's `docker/build-push-action@v7` step:

```yaml
- uses: docker/build-push-action@v7
  with:
    context: ./api
    push: true
    build-args: |
      SPRING_DATA_REDIS_PASSWORD=${{ secrets.SPRING_DATA_REDIS_PASSWORD }}
      STACKOVERFLOW_API_KEY=${{ secrets.STACKOVERFLOW_API_KEY }}
    tags: |
      ghcr.io/${{ needs.namespace.outputs.namespace }}/swtp-api:${{ github.ref_name == 'main' && 'latest' || 'dev' }}
      ghcr.io/${{ needs.namespace.outputs.namespace }}/swtp-api:${{ github.run_number }}
```

**How it works:**
1. CI/CD detects backend change → builds Docker image
2. Passes `SPRING_DATA_REDIS_PASSWORD` and `STACKOVERFLOW_API_KEY` as build args
3. Dockerfile accepts them as `ARG` and exports as `ENV`
4. Spring Boot reads env vars at runtime → uses them in `application.yaml`
5. No server compose file changes needed — ever

---

## 5. Local Development

Without access to the Redis server, start the backend with caching disabled:

```bash
SPRING_CACHE_TYPE=none ./mvnw spring-boot:run
```

Or run Redis locally via Docker:

```bash
docker run -d -p 6379:6379 redis:7-alpine
export SPRING_DATA_REDIS_HOST=localhost
./mvnw spring-boot:run
```

---

## 7. Known Limitations

| Limitation | Impact | Why Accepted |
|------------|--------|-------------|
| **`backoff` not respected** | If SO API returns `backoff: 5`, the next immediate call may be throttled. | Caching means sequential API calls are extremely rare (only on cache miss for a new tag). |

## 8. Edge Cases

| Scenario | Behavior |
|----------|----------|
| Tag exists in local DB | Returned immediately, no API call |
| Tag valid on SO, first use | API call → Redis cache → create in DB |
| Tag not valid on SO | `400 Bad Request` |
| SO API is down | `502 Bad Gateway` (fail-closed) |
| API quota exhausted | Logged → subsequent calls fail → 502 |
| Redis connection refused | Depends on `CacheErrorHandler` config |
| Invalid characters in name | Caught by `@Pattern` DTO validation first |
| Blank / too long name | Caught by `@NotBlank` / `@Size` DTO validation |

---

## 9. Implementation Order

| # | Step | Files |
|---|------|-------|
| 1 | Maven deps | `pom.xml` |
| 2 | `application.yaml` config | `application.yaml` |
| 3 | `CacheConfig.java` | New |
| 4 | `TagSource.java` | New |
| 5 | `StackOverflowTagSource.java` | New |
| 6 | `TagValidationService.java` | New |
| 7 | `TagNotValidException.java` + `TagValidationException.java` | New |
| 8 | Modify `ProjectTagService` | Edit |
| 9 | Modify `ProfileTagService` | Edit |
| 10 | Add handlers to `GlobalExceptionHandler` | Edit |
| 11 | `Dockerfile` — add build args | Edit |
| 12 | `cd-build-deploy.yml` — add build-args to backend job | Edit |
| 13 | Frontend error handling | Edit `tag-list.ts`, `profile-tag-list.component.ts` |
| 14 | Frontend i18n keys | Edit `en.json`, `de.json` |

---

## 10. Files Changed Summary

| Type | Count | Files |
|------|-------|-------|
| **New (backend)** | 6 | `CacheConfig.java`, `TagSource.java`, `StackOverflowTagSource.java`, `TagValidationService.java`, `TagNotValidException.java`, `TagValidationException.java` |
| **Modified (backend)** | 7 | `pom.xml`, `application.yaml`, `ProjectTagService.java`, `ProfileTagService.java`, `GlobalExceptionHandler.java`, `Dockerfile`, `cd-build-deploy.yml` |
| **Modified (frontend)** | 4 | `tag-list.ts`, `profile-tag-list.component.ts`, `en.json`, `de.json` |
| **Total** | 17 | All codebase — zero server changes |
