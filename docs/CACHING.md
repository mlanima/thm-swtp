# Caching Strategy

## Current State

One cache exists: `tag-exists` (Redis, 1h TTL). Only invalid tags (`false`) are cached via `unless = "#result"`. Caching is optional — the `CacheManager` falls back to `NoOpCacheManager` when Redis is unreachable.

---

## Candidates

### Tier 1 — Every-Request Overhead

| Cache Name | What | Why | TTL | Evict On |
|---|---|---|---|---|
| `user-bans` | `BannedUserFilter` / `SecurityService.isActiveUser()` — checks if user is banned | Runs on every authenticated request; one DB query per request | 30s | `banUser`, `unbanUser` |
| `project-counts` | `countFavorites(projectId)`, `countViews(projectId)`, `isFavorited(user, project)` | Called inside `ProjectService.toResponse()` — 3+ queries per project response, per search hit | 1min | favorite add/remove, project update |
| `project-url-exists` | `existsByProjectUrl(url)` — URL uniqueness check | Called from `ProjectController.validateProjectUrl()` on every URL input keystroke | 5min | project create, project URL update |

### Tier 2 — Search & Autocomplete

| Cache Name | What | Why | TTL | Evict On |
|---|---|---|---|---|
| `tags-popular` | `TagRepository` — `LEFT JOIN ... GROUP BY ... ORDER BY COUNT` | Aggregation query on every tag autocomplete open (when query is empty) | 1min | new tag created |
| `tags-search` | `TagRepository.findByNameContainingIgnoreCase()` — `LIKE '%query%'` | Every tag autocomplete keystroke | 1min | new tag created |
| `search-projects` | `ProjectSearchRepository.searchIdsByQuery()` + `findAllWithTagsById` | Multi-query `LIKE '%query%'` with ID intersection and batch fetch | 30s | project create/edit/delete |
| `search-users` | `UserSearchRepository` — same pattern as projects | Same as above for user search | 30s | profile update |

### Tier 3 — Social Graph (Profile Pages)

| Cache Name | What | Why | TTL | Evict On |
|---|---|---|---|---|
| `is-following` | `existsByFollowerKeycloakIdAndFollowingKeycloakId()` | Called on every profile page load | 1min | follow, unfollow |
| `follower-counts` | `countByFollowingKeycloakId()` | Displayed on profile page | 1min | follow, unfollow |
| `profile-lookups` | `findByUsername()` / `findByKeycloakId()` | Profile page views, internal lookups | 5min | profile update, profile delete |

---

## Notes

- **Short TTLs** are intentional — avoids stale data without complex eviction wiring
- **Eviction** is preferred over long TTLs for data that changes (favorites, follows, bans)
- **No-op fallback** already handles Redis-unavailable scenarios — caching is always optional
- Aggregation queries (`COUNT`, `GROUP BY`) benefit most because they scan more rows than indexed lookups
