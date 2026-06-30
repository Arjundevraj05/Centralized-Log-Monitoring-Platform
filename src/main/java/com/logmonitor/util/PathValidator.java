package com.logmonitor.util;

import com.logmonitor.exception.InvalidLogPathException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PathValidator {

    private static final Pattern SAFE_ABSOLUTE_PATH =
            Pattern.compile("^/[a-zA-Z0-9._\\-/]+$");
    private static final Pattern SAFE_APP_NAME =
            Pattern.compile("^[a-zA-Z0-9._\\-]+$");

    public void validateAbsolutePath(String path) {
        if (path == null || path.isBlank() || !SAFE_ABSOLUTE_PATH.matcher(path).matches()) {
            throw new InvalidLogPathException("Invalid log path: " + path);
        }
        if (path.contains("..")) {
            throw new InvalidLogPathException("Path traversal is not allowed");
        }
    }

    public void validateCatalinaHome(String path) {
        validateAbsolutePath(path);
        if (!path.contains("apache-tomcat")) {
            throw new InvalidLogPathException("CATALINA_HOME must be under apache-tomcat directory");
        }
    }

    public void validateAppName(String appName) {
        if (appName == null || !SAFE_APP_NAME.matcher(appName).matches()) {
            throw new InvalidLogPathException("Invalid application name: " + appName);
        }
    }

    public void validateArchivedPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        if (pattern.contains("..") || !pattern.startsWith("/")) {
            throw new InvalidLogPathException("Invalid archived log path pattern");
        }
    }

    public String buildLogbackClassesPath(String catalinaHome, String appName) {
        validateCatalinaHome(catalinaHome);
        validateAppName(appName);
        return catalinaHome + "/webapps/" + appName + "/WEB-INF/classes";
    }
}
