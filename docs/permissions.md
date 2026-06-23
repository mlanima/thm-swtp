# Authorization Model

> **Guideline — how to secure a new route**
>
> 1. Add a public method to `SecurityService` that encodes the permission logic
>    (e.g. `canDoSomething(requiredId, authentication)`).
> 2. Reference it from the controller with `@PreAuthorize("@security.canDoSomething(...)")`.
> 3. Keep the `SecurityService` method focused — use the private helpers
>    (`hasModeratorRole`, `isRegularUser`, `isProjectOwner`, etc.) for common
>    checks instead of inlining authority strings.
> 4. Do **not** import `SecurityService` into a service layer — keep all
>    authorization at the controller `@PreAuthorize` level.
> 5. If the check needs a DB lookup (ownership, membership), do it via a
>    repository query, never by loading full entities.

This document describes the current authorization rules implemented by `SecurityService`.

## Roles

| Role        | Source                       | Meaning                                                                                                 |
|-------------|------------------------------|---------------------------------------------------------------------------------------------------------|
| `USER`      | Added by `KeycloakJwtConverter` for every authenticated user | Regular account; may own a user profile and participate in projects                  |
| `MODERATOR` | Extracted from Keycloak realm role `MODERATOR` in the JWT    | Oversight role; can view and delete any project/user-profile, but cannot own or contribute to projects |
| `Professor` | DB-only boolean on `UserProfile` (assigned by a moderator)   | Flag for future professor-gated features; not checked by any current permission                        |

The `MODERATOR` role is **not** stored in the application database — it is read
directly from the Keycloak JWT's `realm_access.roles` claim by
`KeycloakJwtConverter`. Every authenticated user automatically receives
`ROLE_USER`. If the JWT contains the Keycloak realm role `MODERATOR`, the user
also receives `ROLE_MODERATOR`.

Unlike the old `ADMIN` role, `MODERATOR` accounts **can** create and own user
profiles, but they are excluded from most project-contributor and
profile-owner operations.

> **Frontend note (since 2026-06-23):** The success page (`/success`) checks
> `isModerator()` and skips the `getMyProfile()` call for moderators, so no
> `UserProfile` is created when a moderator logs in. Moderators therefore have
> **no profile** in the application database. This is intentional — the moderator page
> (`/moderator`) is their landing page, and they do not need a profile for moderation
> tasks.

## Roles and Relationships

| Term           | Meaning                                                                                                       |
|----------------|---------------------------------------------------------------------------------------------------------------|
| `Regular user` | Authenticated user with `ROLE_USER` and **without** `ROLE_MODERATOR`                                          |
| `Moderator`    | Authenticated user with `ROLE_MODERATOR` (and always `ROLE_USER`)                                            |
| Profile owner  | Regular user whose Keycloak ID belongs to the profile                                                         |
| Project owner  | Regular user stored as the owner of a project                                                                 |
| Project member | Regular user contained in the project's member list                                                           |
| Contributor    | Regular user who is a project owner or member                                                                 |
| Post author    | Contributor stored as the author of a post                                                                    |
| Invited user   | Regular user who is the recipient of a project invitation                                                     |

A moderator may inspect and delete projects and user profiles for moderation
purposes, but cannot own a project, become a project member, or perform
contributor actions.

## Projects

| Action                            | Security method          | Regular user               | Project member             | Project owner              | Moderator                   |
|-----------------------------------|--------------------------|----------------------------|----------------------------|----------------------------|-----------------------------|
| View a public project             | `canViewProject`         | Allowed                    | Allowed                    | Allowed                    | Allowed                     |
| View a private project            | `canViewProject`         | Denied                     | Allowed                    | Allowed                    | Allowed                     |
| View a project by URL             | `canViewProjectByUrl`    | Follows project visibility | Follows project visibility | Follows project visibility | Follows project visibility  |
| Create a project                  | `canCreateProject`       | Allowed                    | Allowed                    | Allowed                    | Denied                      |
| Edit a project                    | `canEditProject`         | Denied                     | Denied                     | Allowed                    | Denied                      |
| Delete a project                  | `canDeleteProject`       | Denied                     | Denied                     | Allowed                    | Allowed                     |
| View members of a public project  | `canViewProjectMembers`  | Allowed                    | Allowed                    | Allowed                    | Allowed                     |
| View members of a private project | `canViewProjectMembers`  | Denied                     | Allowed                    | Allowed                    | Allowed                     |
| Remove a project member           | `canRemoveProjectMember` | Denied                     | Denied                     | Allowed                    | Denied                      |

The project owner cannot remove themselves through the project-member endpoint.

## Project Links

| Action                | Security method  | Rule                                          |
|-----------------------|------------------|-----------------------------------------------|
| View project links    | `canViewProject` | Follows project visibility                    |
| Create a project link | `canEditProject` | Project owner only; moderators are denied     |
| Edit a project link   | `canEditProject` | Project owner only; moderators are denied     |
| Delete a project link | `canEditProject` | Project owner only; moderators are denied     |

## Project Files

| Action             | Security method  | Rule                                          |
|--------------------|------------------|-----------------------------------------------|
| View the file list | `canViewProject` | Follows project visibility                    |
| Download a file    | `canViewProject` | Follows project visibility                    |
| Upload a file      | `canEditProject` | Project owner only; moderators are denied     |
| Delete a file      | `canEditProject` | Project owner only; moderators are denied     |

## Project Tags

| Action               | Security method  | Rule                                          |
|----------------------|------------------|-----------------------------------------------|
| View project tags    | `canViewProject` | Follows project visibility                    |
| Add a project tag    | `canEditProject` | Project owner only; moderators are denied     |
| Remove a project tag | `canEditProject` | Project owner only; moderators are denied     |

## Project Invitations

| Action                         | Security method              | Regular user      | Project member    | Project owner     | Moderator | Additional condition                  |
|--------------------------------|------------------------------|-------------------|-------------------|-------------------|-----------|---------------------------------------|
| Create an invitation           | `canCreateProjectInvitation` | Denied            | Denied            | Allowed           | Denied    | The recipient must be a regular user  |
| View invitations for a project | `canViewProjectInvites`      | Denied            | Denied            | Allowed           | Denied    | None                                  |
| Accept or reject an invitation | `canRespondToProjectInvite`  | Only as recipient | Only as recipient | Only as recipient | Denied    | User must be the invitation recipient |

The recipient-role check is performed in the invitation service because `canCreateProjectInvitation` only receives the project ID and the sender's authentication.

## Project Join Requests

| Action                           | Security method                | Regular user | Project member | Project owner | Moderator | Additional condition                               |
|----------------------------------|--------------------------------|--------------|----------------|---------------|-----------|----------------------------------------------------|
| Create a join request            | `canCreateProjectJoinRequest`  | Allowed      | Denied         | Denied        | Denied    | User must not already be a contributor             |
| View join requests for a project | `canViewProjectJoinRequests`   | Denied       | Denied         | Allowed       | Denied    | None                                               |
| Accept or reject a join request  | `canManageProjectJoinRequests` | Denied       | Denied         | Allowed       | Denied    | Request must belong to a project owned by the user |

The service additionally checks whether join requests are enabled and whether an active request already exists.

## Project Posts

| Action                                    | Security method         | Unrelated user | Project member | Project owner  | Post author                     | Moderator |
|-------------------------------------------|-------------------------|----------------|----------------|----------------|---------------------------------|-----------|
| View published posts of a public project  | `canViewProject`        | Allowed        | Allowed        | Allowed        | Allowed                         | Allowed   |
| View published posts of a private project | `canViewProject`        | Denied         | Allowed        | Allowed        | Allowed when also a contributor | Allowed   |
| Create a post                             | `canCreateProjectPost`  | Denied         | Allowed        | Allowed        | Allowed when also a contributor | Denied    |
| Archive a post                            | `canArchiveProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied    |
| Publish a draft                           | `canPublishProjectPost` | Denied         | Only as author | Only as author | Allowed                         | Denied    |
| Republish an archived post                | `canPublishProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied    |
| Publish an already published post         | `canPublishProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied    |
| Delete a post                             | `canDeleteProjectPost`  | Denied         | Only as author | Allowed        | Allowed                         | Denied    |

Authorship takes precedence for drafts: a project owner cannot publish another user's draft.

## User Profiles

| Action                                                     | Security method        | Unrelated user             | Profile owner              | Moderator                           |
|------------------------------------------------------------|------------------------|----------------------------|----------------------------|-------------------------------------|
| View a public profile                                      | None                   | Allowed when authenticated | Allowed when authenticated | Allowed when authenticated          |
| View a user's projects through the protected profile route | `canViewUserProjects`  | Denied                     | Allowed                    | Allowed                             |
| Create or synchronize an own profile                       | None; `/api/users/me`  | Allowed                    | Allowed                    | Allowed                             |
| Edit the profile                                           | `canEditUserProfile`   | Denied                     | Allowed                    | Denied                              |
| Delete the profile                                         | `canDeleteUserProfile` | Denied                     | Allowed                    | Allowed                             |

Moderators **could** create their own profile (the old ADMIN block was removed),
but cannot edit it because profile-owner checks require `isRegularUser`. As of
2026-06-23 the frontend skips profile creation for moderators entirely (see
`success.component.ts`), so moderators do **not** have a profile in the
application database. Deleting a profile no longer touches any user-account
table.

## User Profile Links

| Action                | Security method             | Unrelated user             | Profile owner              | Moderator                           |
|-----------------------|-----------------------------|----------------------------|----------------------------|-------------------------------------|
| View profile links    | None                        | Allowed when authenticated | Allowed when authenticated | Allowed when authenticated          |
| Create a profile link | `canCreateUserProfileLinks` | Denied                     | Allowed                    | Denied                              |
| Edit a profile link   | `canEditUserProfileLinks`   | Denied                     | Allowed                    | Denied                              |
| Delete a profile link | `canDeleteUserProfileLinks` | Denied                     | Allowed                    | Denied                              |

## User Profile Tags

| Action               | Security method            | Rule                                                       |
|----------------------|----------------------------|------------------------------------------------------------|
| View profile tags    | None                       | Readable by any authenticated user                         |
| Add a profile tag    | None; `/users/me` endpoint | Regular profile owner only; the user ID comes from the JWT |
| Remove a profile tag | None; `/users/me` endpoint | Regular profile owner only; the user ID comes from the JWT |

## Project Favorites

| Action                                   | Security method      | Regular user | Project member | Project owner  | Moderator |
|------------------------------------------|----------------------|--------------|----------------|----------------|-----------|
| Favorite an accessible project           | `canFavoriteProject` | Allowed      | Allowed        | Allowed        | Denied    |
| Favorite an inaccessible private project | `canFavoriteProject` | Denied       | Not applicable | Not applicable | Denied    |
| Remove an own favorite                   | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |
| Check an own favorite                    | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |
| View own favorites                       | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |

A project can only be favorited when `canViewProject` succeeds. The authenticated user must also be a regular user (not a moderator).

## Search and Global Tag Catalog

| Area               | Security method | Rule                                                                                                      |
|--------------------|-----------------|-----------------------------------------------------------------------------------------------------------|
| Project search     | None            | Returns only public, non-deleted projects                                                                 |
| User search        | None            | Returns profile data intended for public display; moderator accounts have a profile and are therefore included |
| Global tag catalog | None            | Read-only access                                                                                          |

These endpoints still require authentication under the global security configuration unless explicitly exposed below `/api/public/**`.

