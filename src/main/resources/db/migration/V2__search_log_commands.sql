-- Whitelisted search commands using a fixed placeholder replaced server-side after sanitization.
-- Placeholder __SEARCH_TERM__ is replaced in LogService; user input never forms the command directly.

INSERT INTO log_config (log_name, command_key, command_text) VALUES
    ('Tomcat Catalina Search', 'TOMCAT_LOG_SEARCH',
     'grep -Fi ''__SEARCH_TERM__'' /var/log/tomcat/catalina.out | tail -n 500'),
    ('Tomcat Error Search', 'ERROR_LOG_SEARCH',
     'grep -Fi ''__SEARCH_TERM__'' /var/log/tomcat/localhost.log | tail -n 500'),
    ('Tomcat Access Search', 'ACCESS_LOG_SEARCH',
     'grep -Fi ''__SEARCH_TERM__'' /var/log/tomcat/localhost_access_log.txt | tail -n 500'),
    ('System Messages Search', 'SYSLOG_SEARCH',
     'grep -Fi ''__SEARCH_TERM__'' /var/log/messages | tail -n 500');
