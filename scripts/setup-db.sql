-- Run once as the postgres superuser (password from PostgreSQL install).
-- Example:
--   psql -U postgres -h localhost -f scripts/setup-db.sql

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'logmonitor') THEN
        CREATE USER logmonitor WITH ENCRYPTED PASSWORD 'logmonitor';
    END IF;
END
$$;

SELECT 'CREATE DATABASE logmonitor OWNER logmonitor'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'logmonitor')\gexec

GRANT ALL PRIVILEGES ON DATABASE logmonitor TO logmonitor;
