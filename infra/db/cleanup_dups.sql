-- ============================================================
-- IdeaCamp – Duplicate Cleanup Script
-- Run this BEFORE the app starts if Hibernate fails to apply
-- unique constraints due to existing duplicate data.
--
-- Safe to run multiple times (idempotent).
-- Compatible with MySQL 8+ and SQLite 3.25+.
-- Uses window functions to avoid MySQL error 1093.
-- ============================================================


-- ── 1. projects: duplicate name ──────────────────────────────
DELETE FROM projects WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY name ORDER BY created_at) AS rn
        FROM projects
    ) AS ranked WHERE rn = 1
);

-- ── 2. projects: duplicate project_url ───────────────────────
DELETE FROM projects WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY project_url ORDER BY created_at) AS rn
        FROM projects
    ) AS ranked WHERE rn = 1
);

-- ── 3. project_members: duplicate (project_id, user) ─────────
-- Temp-table approach: compatible with MySQL and SQLite.
CREATE TEMPORARY TABLE project_members_clean AS
    SELECT DISTINCT project_id, user_profile_keycloak_id FROM project_members;
DELETE FROM project_members;
INSERT INTO project_members SELECT * FROM project_members_clean;
DROP TABLE project_members_clean;

-- ── 4. project_invitations: duplicate (project_id, invited_user_id, status) ──
DELETE FROM project_invitations WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id, invited_user_id, status ORDER BY created_at) AS rn
        FROM project_invitations
    ) AS ranked WHERE rn = 1
);

-- ── 5. project_join_requests: duplicate (project_id, requesting_user_id, status) ──
DELETE FROM project_join_requests WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id, requesting_user_id, status ORDER BY created_at) AS rn
        FROM project_join_requests
    ) AS ranked WHERE rn = 1
);

-- ── 6. project_links: duplicate (project_id, url) ────────────
DELETE FROM project_links WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY project_id, url ORDER BY created_at) AS rn
        FROM project_links
    ) AS ranked WHERE rn = 1
);

-- ── 7. user_profile_links: duplicate (user_profile_keycloak_id, url) ──
DELETE FROM user_profile_links WHERE id NOT IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY user_profile_keycloak_id, url ORDER BY created_at) AS rn
        FROM user_profile_links
    ) AS ranked WHERE rn = 1
);
