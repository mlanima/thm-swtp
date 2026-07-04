# Content Moderation

Extending the existing OpenAI Moderation infrastructure (from tag validation) to moderate free-text user content — bios, descriptions, posts, and messages.

---

## Rationale

Tag validation already uses `OpenAIModerationClient` to flag single words via the OpenAI Moderation API. The same client can be reused to moderate longer, multi-sentence content. Unlike tags (which are cached permanently in the DB once valid), content is more dynamic and longer — caching is per-content hash with shorter TTL.

---

## Fields That Can Be Moderated

Grouped by priority and implementation effort:

### Tier 1 — Bio / Description (Low Effort, High Impact)

| Field | Entity | Backend Entry Point | Max Length | Existing Validation |
|---|---|---|---|---|
| `UserProfile.about` | `UserProfile` | `UserProfileController.updateProfile()` → `UserProfileService.updateProfile()` | `TEXT` (unbounded) | None (no `@Valid`, no DB constraint beyond `TEXT`) |
| `UserProfile.experience` | `UserProfile` | Same as above | `TEXT` (unbounded) | None |
| `Project.description` | `ProjectEntity` | `ProjectController.createProject()` / `editProject()` → `ProjectService` | `@Column(length = 500)` | DB length only; no `@Valid` on controller |
| `Project.shortDescription` | `ProjectEntity` | Same as above | `@Column(length = 200)` | DB length only; no `@Valid` on controller |

### Tier 2 — Posts (Medium Effort, High Impact)

| Field | Entity | Backend Entry Point | Max Length | Existing Validation |
|---|---|---|---|---|
| `ProjectPost.title` | `ProjectPostEntity` | `ProjectPostController.createPost()` | 200 | `@NotBlank @Size` + `@Valid` on controller |
| `ProjectPost.content` | `ProjectPostEntity` | Same as above | 10,000 | `@NotBlank @Size` + `@Valid` on controller |

### Tier 3 — Messages (Medium Effort, Medium Impact)

| Field | Entity | Backend Entry Point | Max Length | Existing Validation |
|---|---|---|---|---|
| `ProjectJoinRequest.message` | `ProjectJoinRequestEntity` | `ProjectJoinRequestController.createRequest()` | 500 | `@Size` + `@Valid` |
| `ProjectInvite.message` | `ProjectInviteEntity` | `ProjectInviteController.createInvite()` | 500 | `@Size` + `@Valid` |
| `ProfessorRequest.text` | `ProfessorRequestEntity` | `ProfessorRequestController.createRequest()` | 1000 | `@NotBlank @Size` + `@Valid` |

### Tier 4 — Misc (Low Effort, Low Impact)

| Field | Entity | Backend Entry Point | Max Length | Existing Validation |
|---|---|---|---|---|
| `UserProfile.title` | `UserProfile` | `UserProfileController.updateProfile()` | None | None |
| `UserProfile.location` | `UserProfile` | Same as above | None | None |

---

## Architecture

```
ModerationClient (moved from tag.validation)
        │
        ├── TagValidationService          # existing use — cached tag checking
        ├── ContentModerationService      # new — moderated free-text content
        │       │
        │       ├── UserProfileService        # moderate about/experience
        │       ├── ProjectService            # moderate description/shortDescription
        │       ├── ProjectPostService        # moderate title/content
        │       ├── ProjectJoinRequestService # moderate message
        │       ├── ProjectInviteService      # moderate message
        │       └── ProfessorRequestService   # moderate text
        │
        └── BlocklistService (fallback)  # LDNOOBW wordlist — shared with tag validation
```

### Component Overview

**`ModerationClient`** — the existing `OpenAIModerationClient` moved from `tag.validation` to `moderation`, renamed to `ModerationClient`. Already fully generic (accepts any `String input`). Supports:
- Configurable model (`omni-moderation-latest`)
- Configurable threshold (default `0.1`)
- Throws `ModerationApiException` (renamed from `TagValidationException`) when unavailable
- Automatic disable when no API key is configured

The move removes the awkward cross-package dependency and eliminates the misleading `TagValidationException` name for non-tag use.

**`ContentModerationService`** — `@Cacheable` wrapper similar to `TagValidationService`, but:
- Cache key = SHA-256 hash of the content (to handle long strings)
- Cache TTL = shorter (e.g., 10 minutes instead of 1 hour), since content is more varied than tags
- Cache only flagged results (`unless = "#result == false"`)
- Catches `ModerationApiException` from the client and re-throws as `ContentModerationException`

**Exception handling** — Reuse the same pattern:
- `ContentNotValidException` → `400 Bad Request` with code `CONTENT_NOT_VALID`
- `ContentModerationException` (OpenAI down) → `502 Bad Gateway`
- (Optional) Fallback to `BlocklistService.contains()` on API failure, mirroring the tag source pattern

---

## Data Flow

```
User submits "I am a great developer"
  → UserProfileService.updateProfile()
    → contentModerationService.isContentAppropriate("I am a great developer")
      → @Cacheable(value = "content-moderation", key = "#hash")
        → REDIS MISS → ModerationClient.isFlagged("I am a great developer")
          → OpenAI returns flagged=false, all scores < 0.1
        → NOT FLAGGED → return true (not cached)
    → OK → save profile

User submits offensive text
  → ...contentModerationService.isContentAppropriate(offensive)
    → ModerationClient.isFlagged(offensive)
      → OpenAI returns flagged=true, score > 0.1
    → FLAGGED → return false (cached in Redis with TTL)
    → throw ContentNotValidException → 400 Bad Request

OpenAI is down
  → ...contentModerationService.isContentAppropriate(text)
    → ModerationClient.isFlagged(text)
      → ResourceAccessException / ModerationApiException
    → (optional) BlocklistService.contains(text)? fallback
    → throw ContentModerationException → 502 Bad Gateway
```

---

## Where to Integrate

### 1. User Profile — `about`, `experience`, `title`, `location`

**Service:** `UserProfileService.updateProfile()` (`api/src/main/java/.../userprofile/service/UserProfileService.java:58`)

Before setting fields on the entity:
```java
@Transactional
public UserProfile updateProfile(String username, String title, String location,
                                  String about, String experience) {
    // Moderate free-text fields
    contentModerationService.assertAppropriate(about, "about");
    contentModerationService.assertAppropriate(experience, "experience");

    UserProfile profile = findOrThrow(username);
    profile.setAbout(about);
    profile.setExperience(experience);
    ...
}
```

### 2. Project — `description`, `shortDescription`

**Service:** `ProjectService.createProject()` (line 82) and `ProjectService.editProject()` (line 215)

For creation:
```java
contentModerationService.assertAppropriate(request.description(), "description");
contentModerationService.assertAppropriate(request.shortDescription(), "shortDescription");
```

For editing, moderate only if the value changed.

### 3. Project Posts — `title`, `content`

**Service:** `ProjectPostService` (in `projectPost/`)

### 4. Join Requests, Invites, Professor Requests — `message` / `text`

**Services:** Respective service classes under `projectJoinRequest/`, `projectInvitation/`, `professorRequest/`

---

## Configuration

Add to `application.yaml`:

```yaml
app:
  content-moderation:
    enabled: true                    # master switch; can disable per-Tier in the future
    cache-ttl: 10m                   # Redis TTL for flagged content
    blocklist-fallback: true         # fall back to BlocklistService when OpenAI is down
```

The existing `openai.moderation.*` settings are reused:

```yaml
openai:
  moderation:
    model: omni-moderation-latest
    threshold: 0.1
```

No new API keys required — the existing `OPENAI_API_KEY` env var is shared.

---

## Caching

Flagged (inappropriate) content → cached in Redis to avoid repeated API calls. Keyed by SHA-256 hash of the content.

```java
@Cacheable(value = "content-moderation", key = "#root.target.toHash(#content)", unless = "#result")
public boolean isContentAppropriate(String content) { ... }
```

Cache config addition in `CacheConfig.java`:

```java
private RedisCacheConfiguration contentModerationCacheConfig() {
    return defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .prefixCacheNameWith("content:");
}
```

---

## File Layout (proposed)

```
api/src/main/java/de/thm/swtp/api/
├── moderation/
│   ├── ModerationClient.java              # ← moved from tag.validation, renamed
│   ├── exception/ModerationApiException.java  # ← renamed from TagValidationException
│   ├── ContentModerationService.java       # @Cacheable wrapper
│   ├── exception/ContentNotValidException.java
│   └── exception/ContentModerationException.java
├── tag/validation/
│   ├── TagSource.java                     # unchanged
│   ├── ...TagSource implementations       # ← update import to moderation.ModerationClient
│   ├── TagValidationService.java          # unchanged
│   └── BlocklistService.java              # unchanged (shared, stays in tag.validation)
└── exceptionhandling/GlobalExceptionHandler.java  # add handlers + update TagValidationException → ModerationApiException reference
```

After the move, `ContentModerationService` injects `ModerationClient` from the same package:

```java
@Service
public class ContentModerationService {

    private final ModerationClient moderationClient;
    private final BlocklistService blocklistService;
    private final boolean blocklistFallback;

    @Cacheable(value = "content-moderation", key = "#hash(content)", unless = "#result")
    public boolean isContentAppropriate(String content) {
        try {
            return !moderationClient.isFlagged(content);
        } catch (ModerationApiException e) {
            if (blocklistFallback) {
                return !blocklistService.containsAny(content);
            }
            throw new ContentModerationException("Content moderation temporarily unavailable");
        }
    }

    public void assertAppropriate(String content, String fieldName) {
        if (content == null || content.isBlank()) return;
        if (!isContentAppropriate(content)) {
            throw new ContentNotValidException(fieldName);
        }
    }

    String hash(String content) {
        return DigestUtils.sha256Hex(content != null ? content : "");
    }
}
```

Existing tag source implementations (`OpenAIModeratedGithubTagSource` etc.) update their import from `tag.validation.OpenAIModerationClient` to `moderation.ModerationClient` and from `TagValidationException` to `ModerationApiException`. No behavioral changes.

---

## Error Handling

| Exception | HTTP | Log Level | Message |
|-----------|------|-----------|---------|
| `ContentNotValidException` | 400 | debug | `"Content in field 'about' is not appropriate"` |
| `ContentModerationException` | 502 | error | `"Content moderation service temporarily unavailable"` |

Add to `GlobalExceptionHandler`:

```java
@ExceptionHandler(ContentNotValidException.class)
public ResponseEntity<ErrorResponse> handleContentNotValid(ContentNotValidException ex) {
    log.debug("Bad Request (400): {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), "CONTENT_NOT_VALID"));
}

@ExceptionHandler(ContentModerationException.class)
public ResponseEntity<ErrorResponse> handleContentModerationError(ContentModerationException ex) {
    log.error("Content moderation failed: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of(502, "Bad Gateway", "Content moderation service temporarily unavailable."));
}
```

---

## Frontend Considerations

The existing tag validation frontend error handling checks `err.status === 400` and the `"not a valid"` substring. For content moderation, a similar pattern can be used — check `err.status === 400` and the `errorCode === "CONTENT_NOT_VALID"` on the error response body.

| Feature | File | Current Error Handling | Changes Needed |
|---------|------|------------------------|----------------|
| User profile save | `user-profile.ts:229` | Generic error message | Check `errorCode === "CONTENT_NOT_VALID"` and show field-specific translation key |
| Project create | `project-create` component | Zod validation errors | Add server-side moderation error handling |
| Project edit (site) | `project-site.ts:116` | Generic error message | Check for CONTENT_NOT_VALID |

---

## Implementation Order (Recommended)

1. **Refactor** — Move `OpenAIModerationClient` → `moderation.ModerationClient`, rename `TagValidationException` → `ModerationApiException`, update imports in tag sources. This is a mechanical change with no behavior difference, but it keeps the package boundary clean before adding new code.

2. **Tier 1** — Add `ContentModerationService`, integrate in `UserProfileService.updateProfile()` and `ProjectService` (create + edit). This covers the originally requested fields with moderate effort.

3. **Tier 2** — Integrate in `ProjectPostService` for post title/content (highest abuse risk among remaining fields).

4. **Tier 3** — Integrate in join request, invite, and professor request services (lower frequency, lower risk).

5. **Tier 4** — Optionally moderate profile title and location (low risk, quick win).
