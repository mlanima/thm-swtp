# Content Moderation & Location

Two features built on top of the same OpenAI Moderation API infrastructure:

1. **Content Moderation** — free-text moderation for user profiles, projects, and posts via OpenAI Omni Moderation, with an LDNOOBW blocklist as fallback
2. **Google Places Location** — placeId-based city validation for user profile locations via the Google Place Details API

Both return clear error codes — `CONTENT_NOT_VALID` (400) and `INVALID_PLACE` (400) — that the frontend displays as toast notifications.

---

## Architecture

```
ModerationClient (OpenAI /v1/moderations)
        │
        ├── TagValidationService              # tag validation (existing)
        │       └── TagSource implementations
        │
        ├── ContentModerationService           # free-text content moderation
        │       ├── UserProfileService         #   title / about / experience
        │       ├── ProjectService              #   name / description / shortDescription
        │       └── ProjectPostService          #   title / content
        │
        └── BlocklistService (fallback)        # used when OpenAI is unreachable

GooglePlacesClient (Google Place Details API)
        │
        └── UserProfileService                 # validate placeId → canonical location name
```

---

## Content Moderation

### Which Fields Are Moderated

| Entity | Fields | Entry Point |
|--------|--------|-------------|
| `UserProfile` | `title`, `about`, `experience` | `UserProfileService.updateProfile()` |
| `ProjectEntity` | `name`, `description`, `shortDescription` | `ProjectService.createProject()` / `.editProject()` |
| `ProjectPostEntity` | `title`, `content` | `ProjectPostService.createProjectPost()` |

Each field is moderated independently — one flagged field rejects the whole request with a 400 `CONTENT_NOT_VALID` error.

### ContentModerationService

`api/src/main/java/.../moderation/ContentModerationService.java`

Uses **manual `CacheManager` access** (not `@Cacheable`) to log cache hits and misses at `info` level:

```java
public boolean isContentAppropriate(final String content) {
    var key = hash(content);
    var cached = cache != null ? cache.get(key, Boolean.class) : null;
    if (cached != null) {
        log.info("Cache hit for content moderation");
        return cached;
    }
    log.info("Cache miss for content moderation — querying OpenAI");
    var result = checkContent(content);
    if (cache != null) {
        cache.put(key, result);
    }
    return result;
}
```

- **Cache key:** SHA-256 hex of the content (same content from any source = same key)
- **Cache TTL:** 10 minutes
- **Both clean and flagged results are cached** (unlike tag validation which only caches rejected tags)
- **`assertAppropriate(content, fieldName)`** — public API. Skips null/blank silently, logs pass at `info` / reject at `warn`, throws `ContentNotValidException` on rejection

### Blocklist Fallback

Controlled by `app.content-moderation.blocklist-fallback: true`:

```
OpenAI available
  └─ is content flagged? ──yes──→ 400 CONTENT_NOT_VALID
  └─ no ─→ save succeeds

OpenAI down + blocklist-fallback = true
  └─ BlocklistService.containsAny(content)? ──yes──→ 400 CONTENT_NOT_VALID
  └─ no ─→ save succeeds

OpenAI down + blocklist-fallback = false
  └─ throw ContentModerationException ──→ 502 Bad Gateway
```

`BlocklistService` does **whole-word matching** (Unicode-aware) — tokenizes content on Unicode letter boundaries (`\p{L}+`) and checks each token against the blocklist. Lives in `tag/validation/` and is shared with tag validation.

### ModerationClient

`api/src/main/java/.../moderation/ModerationClient.java`

HTTP client for `POST /v1/moderations` (OpenAI). RestClient-based, configurable model (`omni-moderation-latest`) and threshold (default `0.1`). Auto-disabled when `OPENAI_API_KEY` is blank — throws `ModerationApiException`.

### Exceptions

| Exception | HTTP | `errorCode` | When |
|-----------|------|-------------|------|
| `ContentNotValidException` | 400 | `CONTENT_NOT_VALID` | Content flagged by OpenAI or blocklist |
| `ContentModerationException` | 502 | — | OpenAI down and blocklist-fallback off |
| `ModerationApiException` | 502 | — | OpenAI returns 4xx/5xx or unreachable |

---

## Google Places Location

### How It Works

1. User types in the `PlaceAutocomplete` component (frontend)
2. Google Maps Places Autocomplete suggests cities
3. User selects a suggestion → component emits `placeId` + `formatted_address`
4. Both are sent to `PUT /api/v1/user-profiles`
5. Backend calls `GooglePlacesClient.validatePlaceId(placeId)`
6. Google Place Details API returns address components
7. Backend extracts city + country → stores as canonical `location` string
8. Invalid placeId → 400 `INVALID_PLACE`

### GooglePlacesClient

`api/src/main/java/.../location/GooglePlacesClient.java`

```
GET /maps/api/place/details/json
  ?place_id={placeId}
  &key={apiKey}
  &fields=address_components,formatted_address
```

Parses response JSON manually (Jackson 3 — reads as `String` then `readTree()` because `RestClient` can't deserialize abstract `JsonNode`), extracts `locality` or `postal_town` from `address_components`, appends `country`, returns `"Munich, Germany"`. 3s connect / 5s read timeout.

### Location + placeId Pairing

| `location` | `placeId` | Result |
|-----------|-----------|--------|
| non-null | non-null | Validate placeId, set both |
| blank | anything | Clear both (remove location) |
| non-null | null | Skip silently (no change) |
| non-null | `""` | Throw `InvalidPlaceException` (400 INVALID_PLACE) |
| null | anything | Skip silently (no change) |

### Exceptions

| Exception | HTTP | `errorCode` | When |
|-----------|------|-------------|------|
| `InvalidPlaceException` | 400 | `INVALID_PLACE` | placeId doesn't resolve or returns non-OK |
| `GooglePlacesApiException` | 502 | — | Network error or unparseable response |

---

## Caching

| Cache | TTL | Key | Prefix | Values |
|-------|-----|-----|--------|--------|
| `content-moderation` | 10 min | SHA-256 of content | `content:` | Both clean (`true`) and flagged (`false`) |
| `tag-rejected` | 1 h | Tag name | `tags:` | Only rejected (`false`) via `unless = "#result"` |

At startup, pings Redis. If unreachable → `NoOpCacheManager` (no caching, API called on every request). `CacheErrorHandler` logs warnings without crashing.

---

## Frontend

### Toast System

**`ToastService`** — singleton (`providedIn: 'root'`), `toasts` signal, `error()`/`success()`/`warning()`/`info()` methods. Auto-dismiss: errors 6s, warnings 4s, others 3s.

**`ToastComponent`** — fixed top-right, `z-[9999]`, slide-in animation, PrimeIcons per type. Rendered in `app.html` above footer.

### Error Handling by Feature

| Feature | File | Check |
|---------|------|-------|
| Project create | `project-create.ts:146` | `CONTENT_NOT_VALID` → moderation toast |
| Project edit | `project-site.ts:119` | Generic save error toast |
| Profile edit | `user-profile.ts:214-223` | `CONTENT_NOT_VALID` / `INVALID_PLACE` / 401-403 → specific toasts |
| Tag add (project) | `tag-list.ts:68` | `TAG_NOT_VALID` → moderation toast |
| Tag add (profile) | `profile-tag-list.component.ts:63` | `TAG_NOT_VALID` → moderation toast |

### PlaceAutocomplete Component

- Dynamically loads Google Maps JS API (not in `index.html`)
- Restricted to cities (`types: ['(cities)']`)
- Signal-based `input()` / `output()`
- `placeChange` emits `{ placeId, location }` on selection
- Falls back to plain text input if script fails to load

### Project Creation Loading

Finish button shows `pi pi-spin pi-spinner` during request, disabled to prevent double-submit. No top-of-page "Creating..." text.

---

## Configuration

```yaml
app:
  content-moderation:
    blocklist-fallback: true

openai:
  api:
    base-url: https://api.openai.com
    key: ${OPENAI_API_KEY:}
  moderation:
    model: omni-moderation-latest
    threshold: 0.1

google:
  api:
    base-url: https://maps.googleapis.com/maps/api/place
    key: ${GOOGLE_API_KEY:}
```

| Variable | Required For | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | Content moderation | empty (disabled) |
| `GOOGLE_API_KEY` | Location validation (server-side) | empty |
| `GOOGLE_MAPS_API_KEY` | Maps JS / Places Autocomplete (frontend) | empty |

Two separate keys are required because a Google API key can only have one restriction type:
- **`GOOGLE_API_KEY`** — server-side, IP-restricted, used by `GooglePlacesClient`
- **`GOOGLE_MAPS_API_KEY`** — frontend, HTTP referrer-restricted to `*.swtp-ss26.de/*`, `*.review.swtp-ss26.de/*`, `localhost:*`, API-limited to Maps JavaScript API + Places API

### Frontend Environment

```
enviroment.dev.ts   → googleMapsApiKey: ''
enviroment.prod.ts  → googleMapsApiKey: '__GOOGLE_MAPS_API_KEY__' (replaced at Docker build)
```

`angular.json` uses `fileReplacements` to swap `.dev` → `.prod` in production builds.

---

## Deployment

### Frontend — Google Maps API Key (Docker BuildKit Secret)

```dockerfile
RUN --mount=type=secret,id=google-maps-api-key \
    GOOGLE_MAPS_API_KEY=$(cat /run/secrets/google-maps-api-key); \
    sed -i "s|__GOOGLE_MAPS_API_KEY__|${GOOGLE_MAPS_API_KEY}|g" src/app/enviroments/enviroment.prod.ts
```

GitHub Actions pass the secret:
```yaml
secrets: |
  google-maps-api-key=${{ secrets.GOOGLE_MAPS_API_KEY }}
```

The `GOOGLE_MAPS_API_KEY` GitHub Secret must exist in the repository (referrer-restricted for frontend use).

### Backend — API Keys

`OPENAI_API_KEY` and `GOOGLE_API_KEY` (IP-restricted, server-side only) are injected at **runtime** via Docker Compose `.env` — never as build-args.

### Redis

Production at `redis.ser.mlanima.org`. Locally the host is firewalled → `NoOpCacheManager` fallback.

---

## File Layout

```
api/src/main/java/de/thm/swtp/api/
├── moderation/
│   ├── ContentModerationService.java
│   ├── ModerationClient.java
│   └── exception/
│       ├── ContentModerationException.java
│       ├── ContentNotValidException.java
│       └── ModerationApiException.java
├── location/
│   ├── GooglePlacesClient.java
│   └── exception/
│       ├── GooglePlacesApiException.java
│       └── InvalidPlaceException.java
├── config/CacheConfig.java
├── tag/validation/BlocklistService.java
├── userprofile/service/UserProfileService.java
├── project/ProjectService.java
├── projectPost/service/ProjectPostService.java
└── exceptionhandling/GlobalExceptionHandler.java

web/ideacamp/src/app/
├── shared/
│   ├── toast/
│   │   ├── toast.service.ts
│   │   └── toast.ts
│   └── place-autocomplete/
│       ├── place-autocomplete.ts
│       └── place-autocomplete.html
├── feature/
│   ├── project-create/project-create.ts
│   ├── project-site/project-site.ts
│   └── user-profile/user-profile.ts
└── enviroments/
    ├── enviroment.dev.ts
    └── enviroment.prod.ts
```

---

## Adding Moderation to a New Field

1. Inject `ContentModerationService` into the service
2. Call `assertAppropriate(content, fieldName)` before persisting
3. Frontend: check `error.error?.errorCode === 'CONTENT_NOT_VALID'` → show moderation toast

No changes needed to exception handlers, cache config, or the moderation client.

---

## Local Development

- **No Redis** — `NoOpCacheManager` fallback
- **No OpenAI key** — blocklist fallback handles it (returns `false` for clean content)
- **No Google key** — `GooglePlacesClient` fails with 502 if called; autocomplete works as plain text

Both API keys are server-side env vars only — not GitHub Secrets, not in CI/CD.
