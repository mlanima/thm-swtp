# Authorization Model

This document describes the current authorization rules implemented by `SecurityService`.

Admin accounts needs to be created manually.

## Roles and Relationships

| Term           | Meaning                                                                                                       |
|----------------|---------------------------------------------------------------------------------------------------------------|
| `USER`         | Regular account with `ROLE_USER` and without `ROLE_ADMIN`; may own a user profile and participate in projects |
| `ADMIN`        | Administrative account with `ROLE_ADMIN`; has no user profile and cannot participate in projects              |
| Profile owner  | Regular user whose Keycloak ID belongs to the profile                                                         |
| Project owner  | Regular user stored as the owner of a project                                                                 |
| Project member | Regular user contained in the project's member list                                                           |
| Contributor    | Project owner or project member                                                                               |
| Post author    | Contributor stored as the author of a post                                                                    |
| Invited user   | Regular user who is the recipient of a project invitation                                                     |

An admin may inspect and delete projects for moderation purposes, but cannot own a project, become a project member, or perform contributor actions.

## Projects

| Action                            | Security method          | Regular user               | Project member             | Project owner              | Admin                      |
|-----------------------------------|--------------------------|----------------------------|----------------------------|----------------------------|----------------------------|
| View a public project             | `canViewProject`         | Allowed                    | Allowed                    | Allowed                    | Allowed                    |
| View a private project            | `canViewProject`         | Denied                     | Allowed                    | Allowed                    | Allowed                    |
| View a project by URL             | `canViewProjectByUrl`    | Follows project visibility | Follows project visibility | Follows project visibility | Follows project visibility |
| Create a project                  | `canCreateProject`       | Allowed                    | Allowed                    | Allowed                    | Denied                     |
| Edit a project                    | `canEditProject`         | Denied                     | Denied                     | Allowed                    | Denied                     |
| Delete a project                  | `canDeleteProject`       | Denied                     | Denied                     | Allowed                    | Allowed                    |
| View members of a public project  | `canViewProjectMembers`  | Allowed                    | Allowed                    | Allowed                    | Allowed                    |
| View members of a private project | `canViewProjectMembers`  | Denied                     | Allowed                    | Allowed                    | Allowed                    |
| Remove a project member           | `canRemoveProjectMember` | Denied                     | Denied                     | Allowed                    | Denied                     |

The project owner cannot remove themselves through the project-member endpoint.

## Project Links

| Action                | Security method  | Rule                                  |
|-----------------------|------------------|---------------------------------------|
| View project links    | `canViewProject` | Follows project visibility            |
| Create a project link | `canEditProject` | Project owner only; admins are denied |
| Edit a project link   | `canEditProject` | Project owner only; admins are denied |
| Delete a project link | `canEditProject` | Project owner only; admins are denied |

## Project Files

| Action             | Security method  | Rule                                  |
|--------------------|------------------|---------------------------------------|
| View the file list | `canViewProject` | Follows project visibility            |
| Download a file    | `canViewProject` | Follows project visibility            |
| Upload a file      | `canEditProject` | Project owner only; admins are denied |
| Delete a file      | `canEditProject` | Project owner only; admins are denied |

## Project Tags

| Action               | Security method  | Rule                                  |
|----------------------|------------------|---------------------------------------|
| View project tags    | `canViewProject` | Follows project visibility            |
| Add a project tag    | `canEditProject` | Project owner only; admins are denied |
| Remove a project tag | `canEditProject` | Project owner only; admins are denied |

## Project Invitations

| Action                         | Security method              | Regular user      | Project member    | Project owner     | Admin  | Additional condition                  |
|--------------------------------|------------------------------|-------------------|-------------------|-------------------|--------|---------------------------------------|
| Create an invitation           | `canCreateProjectInvitation` | Denied            | Denied            | Allowed           | Denied | The recipient must be a regular user  |
| View invitations for a project | `canViewProjectInvites`      | Denied            | Denied            | Allowed           | Denied | None                                  |
| Accept or reject an invitation | `canRespondToProjectInvite`  | Only as recipient | Only as recipient | Only as recipient | Denied | User must be the invitation recipient |

The recipient-role check is performed in the invitation service because `canCreateProjectInvitation` only receives the project ID and the sender's authentication.

## Project Join Requests

| Action                           | Security method                | Regular user | Project member | Project owner | Admin  | Additional condition                               |
|----------------------------------|--------------------------------|--------------|----------------|---------------|--------|----------------------------------------------------|
| Create a join request            | `canCreateProjectJoinRequest`  | Allowed      | Denied         | Denied        | Denied | User must not already be a contributor             |
| View join requests for a project | `canViewProjectJoinRequests`   | Denied       | Denied         | Allowed       | Denied | None                                               |
| Accept or reject a join request  | `canManageProjectJoinRequests` | Denied       | Denied         | Allowed       | Denied | Request must belong to a project owned by the user |

The service additionally checks whether join requests are enabled and whether an active request already exists.

## Project Posts

| Action                                    | Security method         | Unrelated user | Project member | Project owner  | Post author                     | Admin   |
|-------------------------------------------|-------------------------|----------------|----------------|----------------|---------------------------------|---------|
| View published posts of a public project  | `canViewProject`        | Allowed        | Allowed        | Allowed        | Allowed                         | Allowed |
| View published posts of a private project | `canViewProject`        | Denied         | Allowed        | Allowed        | Allowed when also a contributor | Allowed |
| Create a post                             | `canCreateProjectPost`  | Denied         | Allowed        | Allowed        | Allowed when also a contributor | Denied  |
| Archive a post                            | `canArchiveProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied  |
| Publish a draft                           | `canPublishProjectPost` | Denied         | Only as author | Only as author | Allowed                         | Denied  |
| Republish an archived post                | `canPublishProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied  |
| Publish an already published post         | `canPublishProjectPost` | Denied         | Only as author | Allowed        | Allowed                         | Denied  |
| Delete a post                             | `canDeleteProjectPost`  | Denied         | Only as author | Allowed        | Allowed                         | Denied  |

Authorship takes precedence for drafts: a project owner cannot publish another user's draft.

## User Profiles

| Action                                                     | Security method        | Unrelated user             | Profile owner              | Admin                               |
|------------------------------------------------------------|------------------------|----------------------------|----------------------------|-------------------------------------|
| View a public profile                                      | None                   | Allowed when authenticated | Allowed when authenticated | Allowed when authenticated          |
| View a user's projects through the protected profile route | `canViewUserProjects`  | Denied                     | Allowed                    | Allowed                             |
| Create or synchronize an own profile                       | None; `/api/users/me`  | Allowed                    | Allowed                    | Denied                              |
| Edit the profile                                           | `canEditUserProfile`   | Denied                     | Allowed                    | Denied                              |
| Delete the profile                                         | `canDeleteUserProfile` | Denied                     | Allowed                    | Allowed for administrative deletion |

Admin accounts do not have their own user profile. Deleting a profile also deletes the associated user account.

## User Profile Links

| Action                | Security method             | Unrelated user             | Profile owner              | Admin                               |
|-----------------------|-----------------------------|----------------------------|----------------------------|-------------------------------------|
| View profile links    | None                        | Allowed when authenticated | Allowed when authenticated | Allowed when authenticated          |
| Create a profile link | `canCreateUserProfileLinks` | Denied                     | Allowed                    | Denied                              |
| Edit a profile link   | `canEditUserProfileLinks`   | Denied                     | Allowed                    | Denied                              |
| Delete a profile link | `canDeleteUserProfileLinks` | Denied                     | Allowed                    | Allowed for administrative deletion |

## User Profile Tags

| Action               | Security method            | Rule                                                       |
|----------------------|----------------------------|------------------------------------------------------------|
| View profile tags    | None                       | Readable by any authenticated user                         |
| Add a profile tag    | None; `/users/me` endpoint | Regular profile owner only; the user ID comes from the JWT |
| Remove a profile tag | None; `/users/me` endpoint | Regular profile owner only; the user ID comes from the JWT |

Admin accounts cannot own profile tags because they do not have a user profile.

## Project Favorites

| Action                                   | Security method      | Regular user | Project member | Project owner  | Admin  |
|------------------------------------------|----------------------|--------------|----------------|----------------|--------|
| Favorite an accessible project           | `canFavoriteProject` | Allowed      | Allowed        | Allowed        | Denied |
| Favorite an inaccessible private project | `canFavoriteProject` | Denied       | Not applicable | Not applicable | Denied |
| Remove an own favorite                   | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied |
| Check an own favorite                    | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied |
| View own favorites                       | None; `/me` endpoint | Allowed      | Allowed        | Allowed        | Denied |

A project can only be favorited when `canViewProject` succeeds. The authenticated user must also be a regular `USER`. Admin accounts cannot own favorites because they do not have a user profile.

## Search and Global Tag Catalog

| Area               | Security method | Rule                                                                                                      |
|--------------------|-----------------|-----------------------------------------------------------------------------------------------------------|
| Project search     | None            | Returns only public, non-deleted projects                                                                 |
| User search        | None            | Returns profile data intended for public display; admin accounts have no profile and are therefore absent |
| Global tag catalog | None            | Read-only access                                                                                          |

These endpoints still require authentication under the global security configuration unless explicitly exposed below `/api/public/**`.

