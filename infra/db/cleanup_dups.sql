-- ============================================================
-- IdeaCamp – Duplicate Cleanup Script
-- Run this BEFORE the app starts if Hibernate fails to apply
-- unique constraints due to existing duplicate data.
--
-- Safe to run multiple times (idempotent).
-- Compatible with MySQL and SQLite.
-- ============================================================


-- ── 1. projects: duplicate name ──────────────────────────────
-- Keep the oldest project per name, delete the rest.
DELETE FROM projects
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, name
        FROM projects
        GROUP BY name
    ) AS keep
    JOIN projects p2 ON p2.name = keep.name AND p2.created_at = keep.min_created
);

-- ── 2. projects: duplicate project_url ───────────────────────
DELETE FROM projects
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, project_url
        FROM projects
        GROUP BY project_url
    ) AS keep
    JOIN projects p2 ON p2.project_url = keep.project_url AND p2.created_at = keep.min_created
);

-- ── 3. project_members: duplicate (project_id, user) ─────────
-- Temp-table approach: compatible with MySQL and SQLite.
CREATE TEMPORARY TABLE project_members_clean AS
    SELECT DISTINCT project_id, user_profile_keycloak_id FROM project_members;
DELETE FROM project_members;
INSERT INTO project_members SELECT * FROM project_members_clean;
DROP TABLE project_members_clean;

-- ── 4. project_invitations: duplicate (project_id, invited_user_id, status) ──
DELETE FROM project_invitations
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, project_id, invited_user_id, status
        FROM project_invitations
        GROUP BY project_id, invited_user_id, status
    ) AS keep
    JOIN project_invitations pi2
      ON pi2.project_id    = keep.project_id
     AND pi2.invited_user_id = keep.invited_user_id
     AND pi2.status        = keep.status
     AND pi2.created_at    = keep.min_created
);

-- ── 5. project_join_requests: duplicate (project_id, requesting_user_id, status) ──
DELETE FROM project_join_requests
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, project_id, requesting_user_id, status
        FROM project_join_requests
        GROUP BY project_id, requesting_user_id, status
    ) AS keep
    JOIN project_join_requests jr2
      ON jr2.project_id         = keep.project_id
     AND jr2.requesting_user_id = keep.requesting_user_id
     AND jr2.status             = keep.status
     AND jr2.created_at         = keep.min_created
);

-- ── 6. project_links: duplicate (project_id, url) ────────────
DELETE FROM project_links
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, project_id, url
        FROM project_links
        GROUP BY project_id, url
    ) AS keep
    JOIN project_links pl2
      ON pl2.project_id  = keep.project_id
     AND pl2.url         = keep.url
     AND pl2.created_at  = keep.min_created
);

-- ── 7. user_profile_links: duplicate (user_profile_keycloak_id, url) ──
DELETE FROM user_profile_links
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MIN(created_at) AS min_created, user_profile_keycloak_id, url
        FROM user_profile_links
        GROUP BY user_profile_keycloak_id, url
    ) AS keep
    JOIN user_profile_links ul2
      ON ul2.user_profile_keycloak_id = keep.user_profile_keycloak_id
     AND ul2.url                      = keep.url
     AND ul2.created_at               = keep.min_created
);
