package com.logmonitor.auth;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.LoginRequest;
import com.logmonitor.dto.LoginResponse;
import com.logmonitor.security.JwtUtils;
import com.logmonitor.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtUtils, auditService);
    }

    @Test
    void login_returnsAccessTokenAndAudits() {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "encoded", "admin@test.com", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtUtils.generateToken(principal)).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("admin", "Admin@123"));

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        verify(auditService).auditLogin("admin");
    }
}
