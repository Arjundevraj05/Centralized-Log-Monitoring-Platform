package com.logmonitor.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Utility methods for accessing the current security context.
 */
public final class SecurityUtils {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private SecurityUtils() {
    }

    /**
     * Returns the username of the currently authenticated principal.
     *
     * @return optional containing the username, or empty if unauthenticated
     */
    public static Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails.getUsername());
        }
        if (principal instanceof String username && !ANONYMOUS_USER.equals(username)) {
            return Optional.of(username);
        }
        return Optional.empty();
    }

    /**
     * Returns the username of the current principal, or a fallback value.
     *
     * @param fallback value to use when no authenticated user is present
     * @return current username or fallback
     */
    public static String getCurrentUsernameOrDefault(String fallback) {
        return getCurrentUsername().orElse(fallback);
    }
}
