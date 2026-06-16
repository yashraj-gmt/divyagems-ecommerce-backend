-- ============================================================
-- V2: Seed Roles
-- Inserts the two application roles that all other modules depend on.
-- Uses ON CONFLICT DO NOTHING so this is idempotent.
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',  'Standard customer role — can browse, order, review, and manage their account.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrator role — full access to product management, order management, and analytics.')
ON CONFLICT (name) DO NOTHING;
