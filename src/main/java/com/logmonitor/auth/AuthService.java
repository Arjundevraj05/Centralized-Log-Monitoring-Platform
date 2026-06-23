package com.logmonitor.auth;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.LoginRequest;
import com.logmonitor.dto.LoginResponse;
import com.logmonitor.security.JwtUtils;
import com.logmonitor.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service handling user authentication and JWT token issuance.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils,
                       AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
    }

    /**
     * Authenticates credentials and returns a JWT access token.
     *
     * @param request login credentials
     * @return response containing the access token
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtUtils.generateToken(principal);

        auditService.auditLogin(principal.getUsername());
        log.info("Login successful for user: {}", principal.getUsername());

        return new LoginResponse(accessToken);
    }
}
