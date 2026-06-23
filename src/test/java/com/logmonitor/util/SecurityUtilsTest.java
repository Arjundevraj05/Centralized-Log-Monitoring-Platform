package com.logmonitor.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecurityUtils}.
 */
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsername_returnsUsernameFromUserDetails() {
        UserDetails principal = User.withUsername("testuser")
                .password("secret")
                .roles("ADMIN")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Optional<String> username = SecurityUtils.getCurrentUsername();

        assertThat(username).contains("testuser");
    }

    @Test
    void getCurrentUsername_returnsEmptyForAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThat(SecurityUtils.getCurrentUsername()).isEmpty();
    }

    @Test
    void getCurrentUsernameOrDefault_returnsFallbackWhenUnauthenticated() {
        assertThat(SecurityUtils.getCurrentUsernameOrDefault("SYSTEM")).isEqualTo("SYSTEM");
    }
}
