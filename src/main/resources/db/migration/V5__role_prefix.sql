-- Update existing rows that use unprefixed roles
UPDATE users SET role = 'ROLE_' || role WHERE role NOT LIKE 'ROLE_%';

-- Change default to use ROLE_ prefix
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'ROLE_VIEWER';
