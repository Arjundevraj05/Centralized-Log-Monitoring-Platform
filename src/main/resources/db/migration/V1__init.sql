-- =============================================================================
-- Centralized Log Monitoring Platform - Initial Schema
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Roles
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id         BIGSERIAL    PRIMARY KEY,
    role_name  VARCHAR(20)  NOT NULL UNIQUE,
    CONSTRAINT chk_roles_role_name CHECK (role_name IN ('ADMIN', 'DEV', 'SUPPORT'))
);

COMMENT ON TABLE roles IS 'Application roles for RBAC';
COMMENT ON COLUMN roles.role_name IS 'Role identifier: ADMIN, DEV, or SUPPORT';

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id         BIGSERIAL      PRIMARY KEY,
    username   VARCHAR(50)    NOT NULL UNIQUE,
    password   VARCHAR(255)   NOT NULL,
    email      VARCHAR(100)   NOT NULL UNIQUE,
    enabled    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS 'Platform users authenticated via JWT';
COMMENT ON COLUMN users.password IS 'BCrypt-hashed password';

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

-- -----------------------------------------------------------------------------
-- User-Roles (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

COMMENT ON TABLE user_roles IS 'Maps users to one or more roles';

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- -----------------------------------------------------------------------------
-- Servers (SSH targets)
-- -----------------------------------------------------------------------------
CREATE TABLE servers (
    id                    BIGSERIAL    PRIMARY KEY,
    server_name           VARCHAR(100) NOT NULL,
    host                  VARCHAR(255) NOT NULL,
    port                  INTEGER      NOT NULL DEFAULT 22,
    username              VARCHAR(100) NOT NULL,
    encrypted_private_key TEXT         NOT NULL,
    environment           VARCHAR(50)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_servers_port CHECK (port > 0 AND port <= 65535)
);

COMMENT ON TABLE servers IS 'Remote Linux/Tomcat servers accessible via SSH';
COMMENT ON COLUMN servers.encrypted_private_key IS 'AES-encrypted SSH private key; never logged';

CREATE INDEX idx_servers_host ON servers (host);
CREATE INDEX idx_servers_environment ON servers (environment);
CREATE INDEX idx_servers_active ON servers (active);

-- -----------------------------------------------------------------------------
-- Log command whitelist configuration
-- -----------------------------------------------------------------------------
CREATE TABLE log_config (
    id           BIGSERIAL    PRIMARY KEY,
    log_name     VARCHAR(100) NOT NULL,
    command_key  VARCHAR(50)  NOT NULL UNIQUE,
    command_text TEXT         NOT NULL
);

COMMENT ON TABLE log_config IS 'Whitelisted log commands; only these may be executed via SSH';
COMMENT ON COLUMN log_config.command_key IS 'Lookup key e.g. TOMCAT_LOG, ERROR_LOG, ACCESS_LOG';
COMMENT ON COLUMN log_config.command_text IS 'Pre-approved shell command; no user input permitted';

CREATE INDEX idx_log_config_command_key ON log_config (command_key);

-- -----------------------------------------------------------------------------
-- Audit logs
-- -----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id        BIGSERIAL    PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL,
    action    VARCHAR(100) NOT NULL,
    resource  VARCHAR(255),
    timestamp TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_logs IS 'Immutable audit trail for security-sensitive operations';

CREATE INDEX idx_audit_logs_username ON audit_logs (username);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp DESC);

-- -----------------------------------------------------------------------------
-- Seed data: default roles
-- -----------------------------------------------------------------------------
INSERT INTO roles (role_name) VALUES
    ('ADMIN'),
    ('DEV'),
    ('SUPPORT');

-- -----------------------------------------------------------------------------
-- Seed data: whitelisted log commands (Tomcat examples)
-- -----------------------------------------------------------------------------
INSERT INTO log_config (log_name, command_key, command_text) VALUES
    ('Tomcat Catalina Log',  'TOMCAT_LOG', 'tail -n 1000 /var/log/tomcat/catalina.out'),
    ('Tomcat Error Log',     'ERROR_LOG',  'tail -n 1000 /var/log/tomcat/localhost.log'),
    ('Tomcat Access Log',    'ACCESS_LOG', 'tail -n 1000 /var/log/tomcat/localhost_access_log.txt'),
    ('Tomcat Catalina Tail', 'TOMCAT_TAIL', 'tail -f /var/log/tomcat/catalina.out'),
    ('System Messages',      'SYSLOG',     'tail -n 500 /var/log/messages');
