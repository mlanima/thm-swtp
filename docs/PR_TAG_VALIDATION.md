# Tag Validation – New Feature

Implements tag validation from scratch for `ProjectTagService` and `ProfileTagService`. New tags are checked against external sources before being persisted; existing tags (already in the DB) are accepted without validation. Results are cached in Redis (1h TTL), and the system degrades gracefully when Redis is unreachable.

## Architecture

```
TagSource (interface) ← TagValidationService (@Cacheable) ← ProjectTagService / ProfileTagService
```

Three swappable implementations via `app.tags.source`:

| Mode | Description |
|---|---|
| `github-blocklist` (default) | LDNOOBW blocklist (28 languages) → GitHub Topics API |
| `github` | GitHub Topics API only — no moderation |
| `stackoverflow` | StackExchange API v2.3 |

## Key Decisions

- **LDNOOBW wordlist** (~5k entries, 28 languages) committed to repo — no build-time download needed, avoids hardcoded blocklist that needs manual updates
- **GitHub Topics as default API source** — no API key required, 60 req/hr sufficient with caching
- **`@ConditionalOnProperty` selection** — each source is isolated; switching is a single config change
- **`CacheErrorHandler`** — when Redis is unreachable, logs a warning and calls the source directly
- **Blocklist runs before cache** — blocked terms never pollute Redis

## New Files

| File | Purpose |
|---|---|
| `BlocklistService.java` | Loads LDNOOBW wordlist at startup |
| `GitHubTopicsClient.java` | Shared GitHub Topics API client |
| `GitHubTopicsTagSource.java` | Unmoderated GitHub source |
| `ModeratedTagSource.java` | Default — blocklist + GitHub |
| `scripts/download-bad-words.*` | Manual update tools (not build-time) |
| `src/main/resources/bad-words/*` | 28 LDNOOBW language files, committed |

## Fixes Included

- Replaced `HttpStatus.UNPROCESSABLE_ENTITY` / `PAYLOAD_TOO_LARGE` with `HttpStatus.valueOf()` (deprecated in Spring Boot 4.x)
- Added `TagValidationService` mock to `ProjectTagServiceTest` / `ProfileTagServiceTest` with `isValidTag` returning `true`
- Removed `exec-maven-plugin` profiles — bad-word files committed directly (Docker image lacks `bash`)
- Removed `SPRING_DATA_REDIS_PASSWORD` from Dockerfile and CI — Redis auth disabled at network level

See `docs/TAG_VALIDATION.md` for full details.
