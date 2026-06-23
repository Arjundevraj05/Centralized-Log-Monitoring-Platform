package com.logmonitor.security;

import com.logmonitor.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtUtils}.
 */
class JwtUtilsTest {

    private static final String SECRET = "test-jwt-secret-key-minimum-32-characters-long";

    private JwtUtils jwtUtils;
    private User userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(3_600_000L);

        jwtUtils = new JwtUtils(properties);
        jwtUtils.init();

        userDetails = new User(
                "testuser",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void generateToken_andValidate_succeeds() {
        String token = jwtUtils.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtUtils.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void validateToken_failsForWrongUser() {
        String token = jwtUtils.generateToken(userDetails);
        User otherUser = new User("other", "password", userDetails.getAuthorities());

        assertThat(jwtUtils.validateToken(token, otherUser)).isFalse();
    }

    @Test
    void init_throwsWhenSecretTooShort() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short");
        JwtUtils utils = new JwtUtils(properties);

        assertThatThrownBy(utils::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }
}
