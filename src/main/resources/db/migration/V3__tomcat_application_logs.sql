-- Phase 2: Tomcat instance discovery, applications, and cached logback log paths

CREATE TABLE tomcat_instances (
    id              BIGSERIAL    PRIMARY KEY,
    server_id       BIGINT       NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    instance_name   VARCHAR(255) NOT NULL,
    catalina_home   VARCHAR(512) NOT NULL,
    discovered_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tomcat_instance_server_home UNIQUE (server_id, catalina_home)
);

CREATE INDEX idx_tomcat_instances_server_id ON tomcat_instances (server_id);

COMMENT ON TABLE tomcat_instances IS 'Tomcat installations discovered under ~/local/apache-tomcat-* on a server';
COMMENT ON COLUMN tomcat_instances.catalina_home IS 'Absolute path e.g. /home/user/local/apache-tomcat-9.0.85';

CREATE TABLE tomcat_applications (
    id                  BIGSERIAL    PRIMARY KEY,
    tomcat_instance_id  BIGINT       NOT NULL REFERENCES tomcat_instances (id) ON DELETE CASCADE,
    app_name            VARCHAR(255) NOT NULL,
    discovered_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tomcat_app_instance_name UNIQUE (tomcat_instance_id, app_name)
);

CREATE INDEX idx_tomcat_applications_instance_id ON tomcat_applications (tomcat_instance_id);

CREATE TABLE application_log_configs (
    id                    BIGSERIAL    PRIMARY KEY,
    tomcat_application_id BIGINT       NOT NULL REFERENCES tomcat_applications (id) ON DELETE CASCADE,
    logback_xml           TEXT         NOT NULL,
    current_log_path      VARCHAR(1024) NOT NULL,
    archived_path_pattern VARCHAR(1024),
    refreshed_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_log_config_application UNIQUE (tomcat_application_id)
);

COMMENT ON COLUMN application_log_configs.current_log_path IS 'Active log file from logback <file> element';
COMMENT ON COLUMN application_log_configs.archived_path_pattern IS 'Rolling file pattern with date token e.g. app-%d{yyyy-MM-dd}.log.gz';

-- Whitelisted discovery / read commands (placeholders substituted server-side after validation)
INSERT INTO log_config (log_name, command_key, command_text) VALUES
    ('Tomcat Instance Discovery', 'TOMCAT_DISCOVER',
     'ls -1d __HOME__/local/apache-tomcat-* 2>/dev/null'),
    ('Tomcat Webapp List', 'TOMCAT_LIST_WEBAPPS',
     'ls -1 __CATALINA_HOME__/webapps 2>/dev/null'),
    ('Tomcat Logback Read', 'TOMCAT_READ_LOGBACK',
     'cat __LOGBACK_PATH__/logback.xml 2>/dev/null'),
    ('App Log Live Tail', 'APP_LOG_LIVE',
     'tail -f ''__LOG_PATH__'''),
    ('App Log Current', 'APP_LOG_CURRENT',
     'tail -n 5000 ''__LOG_PATH__'''),
    ('App Log Archived', 'APP_LOG_ARCHIVED',
     'zcat ''__LOG_PATH__'' 2>/dev/null | tail -n 5000');
