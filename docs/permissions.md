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

## Overview
- [Roles](#roles)
- [Roles and Relationships](#roles-and-relationships)
- [Projects](#projects)
- [Project Links](#project-links)
- [Project Files](#project-files)
- [Project Tags](#project-tags)
- [Project Invitations](#project-invitations)
- [Project Join Requests](#project-join-requests)
- [Project Posts](#project-posts)
- [Project Favorites](#project-favorites)
- [User Profiles](#user-profiles)
- [User Profile Links](#user-profile-links)
- [User Profile Tags](#user-profile-tags)
- [Professor Requests](#professor-requests)
- [Theses](#theses)
- [User Management and Bans](#user-management-and-bans)
- [Reports and Audit Logs](#reports-and-audit-logs)
- [GitHub Integration](#github-integration)
- [Search and Global Tag Catalog](#search-and-global-tag-catalog)


## Roles

| Role        | Source                                                                        | Meaning                                                                                                |
|-------------|-------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `USER`      | Added by `KeycloakJwtConverter` for every authenticated user                  | Regular account; may own a user profile and participate in projects                                    |
| `MODERATOR` | Extracted from Keycloak realm role `MODERATOR` in the JWT                     | Oversight role; can view and delete any project/user-profile, but cannot own or contribute to projects |
| `Professor` | DB-only boolean on `UserProfile` (assigned through professor-request process) | Active regular user who can create theses and manage theses they supervise                             |

The `MODERATOR` role is **not** stored in the application database — it is read
directly from the Keycloak JWT's `realm_access.roles` claim by
`KeycloakJwtConverter`. Every authenticated user automatically receives
`ROLE_USER`. If the JWT contains the Keycloak realm role `MODERATOR`, the user
also receives `ROLE_MODERATOR`.

Moderators are not intended to have a user profile. The frontend skips profile synchronization,  
although the backend endpoint does not enforce this restriction.

> **Frontend note (since 2026-06-23):** The success page (`/success`) checks
> `isModerator()` and skips the `getMyProfile()` call for moderators, so no
> `UserProfile` is created when a moderator logs in. Moderators therefore have
> **no profile** in the application database. This is intentional — the moderator page
> (`/moderator`) is their landing page, and they do not need a profile for moderation
> tasks.

## Roles and Relationships

| Term           | Meaning                                                                                                            |
|----------------|--------------------------------------------------------------------------------------------------------------------|
| `Regular user` | Authenticated user with `ROLE_USER`, **without** `ROLE_MODERATOR` and with a user profile whose status is `ACTIVE` |
| `Moderator`    | Authenticated user with `ROLE_MODERATOR` (and always `ROLE_USER`)                                                  |
| Profile owner  | Regular user whose Keycloak ID belongs to the profile                                                              |
| Project owner  | Regular user stored as the owner of a project                                                                      |
| Project member | Regular user contained in the project's member list                                                                |
| Contributor    | Regular user who is a project owner or member                                                                      |
| Post author    | User stored as the original author of the post. Authorship does not grant permissions                              |
| Invited user   | Regular user who is the recipient of a project invitation                                                          |
| Professor      | Regular user whose `UserProfile` has `isProfessor = true`                                                          |

A moderator may inspect and delete projects and user profiles for moderation
purposes, but cannot own a project, become a project member, or perform
contributor actions.

A user with status `BANNED` is not considered a regular user. A banned user is denied all permissions based on `isRegularUser`, including project creation,
contribution actions, profile-owner operations, favorites, reports, GitHub integration and professor features.

## Projects

| Action                            | Security method               | Regular user               | Project member             | Project owner              | Moderator                  |
|-----------------------------------|-------------------------------|----------------------------|----------------------------|----------------------------|----------------------------|
| View a public project             | `canViewProject`              | Allowed                    | Allowed                    | Allowed                    | Allowed                    |
| View a private project            | `canViewProject`              | Denied                     | Allowed                    | Allowed                    | Allowed                    |
| View a project by URL             | `canViewProjectByUrl`         | Follows project visibility | Follows project visibility | Follows project visibility | Follows project visibility |
| Create a project                  | `canCreateProject`            | Allowed                    | Allowed                    | Allowed                    | Denied                     |
| Edit a project                    | `canEditProject`              | Denied                     | Denied                     | Allowed                    | Denied                     |
| Delete a project                  | `canDeleteProject`            | Denied                     | Denied                     | Allowed                    | Allowed                    |
| View members of a public project  | `canViewProjectMembers`       | Allowed                    | Allowed                    | Allowed                    | Allowed                    |
| View members of a private project | `canViewProjectMembers`       | Denied                     | Allowed                    | Allowed                    | Allowed                    |
| Remove a project member           | `canRemoveProjectMember`      | Denied                     | Denied                     | Allowed                    | Denied                     |
| Transfer project ownership        | `canTransferProjectOwnership` | Denied                     | Denied                     | Allowed                    | Denied                     |

The project owner cannot remove themselves through the project-member endpoint.
Only the current project owner is allowed to transfer project ownership.

## Project Links

| Action                     | Security method        | Rule                                          |
|----------------------------|------------------------|-----------------------------------------------|
| View project links         | `canViewProjectLinks`  | Follows project visibility                    |
| View public project links  | `canViewProjectLinks`  | Allowed when the project itself is accessible |
| View private project links | `canViewProjectLinks`  | Project owner or member only                  |
| Create a project link      | `canCreateProjectLink` | Project owner only; moderators are denied     |
| Edit a project link        | `canEditProjectLink`   | Project owner only; moderators are denied     |
| Delete a project link      | `canDeleteProjectLink` | Project owner only; moderators are denied     |

## Project Files

Files additionally carry a per-file `visibility` (`PUBLIC`/`PRIVATE`), independent of the
project's own visibility. A private file is hidden from anyone who is not the project owner
or a member, even if the project itself is public — mirroring [Project Links](#project-links).

| Action                     | Security method          | Rule                                                                                                 |
|----------------------------|--------------------------|------------------------------------------------------------------------------------------------------|
| View the file list         | `canViewProjectFiles`    | Follows project visibility; private files are filtered out for non-contributors in the service layer |
| Download a file            | `canDownloadProjectFile` | Follows project visibility; a private file additionally requires being the project owner/member      |
| Upload a file              | `canCreateProjectFile`   | Project owner only; moderators are denied                                                            |
| Change a file's visibility | `canEditProjectFile`     | Project owner only; moderators are denied                                                            |
| Delete a file              | `canDeleteProjectFile`   | Project owner only; moderators are denied                                                            |

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

Only the current project owner may create and manage project posts. Project members cannot create, archive, publish, edit, or delete posts. 
The stored post author does not grant permissions.

| Action                                    | Security method         | Unrelated user | Project member | Current project owner | Moderator |
|-------------------------------------------|-------------------------|----------------|----------------|-----------------------|-----------|
| View published posts of a public project  | `canViewProject`        | Allowed        | Allowed        | Allowed               | Allowed   |
| View published posts of a private project | `canViewProject`        | Denied         | Allowed        | Allowed               | Allowed   |
| View drafts and archived posts            | `canCreateProjectPost`  | Denied         | Denied         | Allowed               | Denied    |
| Create a post                             | `canCreateProjectPost`  | Denied         | Denied         | Allowed               | Denied    |
| Archive a post                            | `canArchiveProjectPost` | Denied         | Denied         | Allowed               | Denied    |
| Publish a draft                           | `canPublishProjectPost` | Denied         | Denied         | Allowed               | Denied    |
| Republish an archived post                | `canPublishProjectPost` | Denied         | Denied         | Allowed               | Denied    |
| Publish an already published post         | `canPublishProjectPost` | Denied         | Denied         | Allowed               | Denied    |
| Edit a post or its image                  | `canEditProjectPost`    | Denied         | Denied         | Allowed               | Denied    |
| Delete a post                             | `canDeleteProjectPost`  | Denied         | Denied         | Allowed               | Allowed   |

Archive, publish, edit, and delete permissions additionally require that the
post exists and belongs to the supplied project.

After a project ownership transfer, only the new project owner may manage existing posts. 
The previous owner may remain stored as the post author but immediately loses all post-management permissions. 
Moderators may delete existing posts but cannot create, archive, publish, or edit them.


## Project Favorites

| Action                                   | Security method      | Regular user | Project member | Project owner  | Moderator |
|------------------------------------------|----------------------|--------------|----------------|----------------|-----------|
| Favorite an accessible project           | `canFavoriteProject` | Allowed      | Allowed        | Allowed        | Denied    |
| Favorite an inaccessible private project | `canFavoriteProject` | Denied       | Not applicable | Not applicable | Denied    |
| Remove an own favorite                   | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |
| Check an own favorite                    | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |
| View own favorites                       | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied    |

A project can only be favorited when `canViewProject` succeeds. The authenticated user must also be a regular user (not a moderator).

## User Profiles

| Action                                                     | Security method          | Unrelated user             | Profile owner              | Moderator                  |
|------------------------------------------------------------|--------------------------|----------------------------|----------------------------|----------------------------|
| View a public profile                                      | None                     | Allowed when authenticated | Allowed when authenticated | Allowed when authenticated |
| View a user's projects through the protected profile route | `canViewUserProjects`    | Denied                     | Allowed                    | Allowed                    |
| Create or synchronize an own profile                       | None; `/api/v1/users/me` | Allowed                    | Allowed                    | Technically allowed        |
| Edit the profile                                           | `canEditUserProfile`     | Denied                     | Allowed                    | Denied                     |
| Delete the profile                                         | `canDeleteUserProfile`   | Denied                     | Allowed                    | Allowed                    |

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



## Professor Requests
| Action                                       | Security method                  | Regular user                         | Professor | Moderator              |
|----------------------------------------------|----------------------------------|--------------------------------------|-----------|------------------------|
| Create a professor request                   | `canCreateProfessorRequest`      | Allowed when not already a professor | Denied    | Denied                 |
| View a professor request for a specific user | `canViewProfessorRequestForUser` | Allowed only for the own user ID     | Own only  | Allowed for every user |
| View all professor requests                  | `canViewAllProfessorRequests`    | Denied                               | Denied    | Allowed                |
| Accept or reject professor requests          | `canManageProfessorRequests`     | Denied                               | Denied    | Allowed                |

Professor status is stored as `isProfessor` on `UserProfile`. The role is not stored inside Keycloak.

## Theses
| Action                             | Security method           | Authenticated user | Thesis student | Supervising professor | Other professor | Moderator |
|------------------------------------|---------------------------|--------------------|----------------|-----------------------|-----------------|-----------|
| View a thesis                      | `canViewThesis`           | Allowed            | Allowed        | Allowed               | Allowed         | Allowed   |
| View a thesis by URL               | `canViewThesisByUrl`      | Allowed            | Allowed        | Allowed               | Allowed         | Allowed   |
| View all theses                    | `canViewAllTheses`        | Denied             | Denied         | Denied                | Denied          | Allowed   |
| View theses through a user profile | `canViewUserTheses`       | Own profile only   | Own only       | Own profile only      | Own only        | Allowed   |
| Create a thesis                    | `canCreateThesis`         | Denied             | Denied         | Allowed               | Allowed         | Denied    |
| Edit a thesis                      | `canEditThesis`           | Denied             | Denied         | Allowed               | Denied          | Denied    |
| Delete a thesis                    | `canDeleteThesis`         | Denied             | Denied         | Allowed               | Denied          | Allowed   |
| Add or remove thesis students      | `canManageThesisStudents` | Denied             | Denied         | Allowed               | Denied          | Denied    |

The backend authorizes moderators to view all theses and delete theses.
Currently, the thesis-moderation controls are not implemented in the frontend.

## User Management and Bans

| Action             | Security method       | Regular user | Moderator |
|--------------------|-----------------------|--------------|-----------|
| View managed users | `canViewManagedUsers` | Denied       | Allowed   |
| Ban a user         | `canBanUser`          | Denied       | Allowed   |
| Unban a user       | `canUnbanUser`        | Denied       | Allowed   |

## Reports and Audit Logs
| Action                    | Security method    | Regular user | Moderator |
|---------------------------|--------------------|--------------|-----------|
| Create a report           | `canCreateReport`  | Allowed      | Denied    |
| View submitted reports    | `canViewReports`   | Denied       | Allowed   |
| Update or resolve reports | `canManageReports` | Denied       | Allowed   |
| View audit logs           | `canViewAuditLogs` | Denied       | Allowed   |

## GitHub Integration
| Action                              | Security method              | Rule                       |
|-------------------------------------|------------------------------|----------------------------|
| Manage own GitHub connection        | `canManageGithubConnection`  | Active regular user only   | 
| View a project's linked repository  | `canViewProjectGithubRepo`   | Follows project visibility | 
| Link or unlink a project repository | `canManageProjectGithubRepo` | Active project owner only  | 


## Search and Global Tag Catalog

| Area               | Security method | Rule                                             |
|--------------------|-----------------|--------------------------------------------------|
| Project search     | None            | Returns only public, non-deleted projects        |
| User search        | None            | Returns profile data intended for public display |
| Global tag catalog | None            | Read-only access                                 |

These endpoints still require authentication under the global security configuration unless explicitly exposed below `/api/public/**`.

