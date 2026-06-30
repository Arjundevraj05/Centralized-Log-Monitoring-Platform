package com.logmonitor.security;

import com.logmonitor.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies RBAC rules defined in {@link SecurityConfig}.
 */
@WebMvcTest
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loginEndpoint_isPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/servers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    void logSearch_deniedForSupportRole() throws Exception {
        mockMvc.perform(post("/api/logs/search"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEV")
    void logSearch_allowedForDevRole() throws Exception {
        mockMvc.perform(post("/api/logs/search"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditEndpoint_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DEV")
    void auditEndpoint_deniedForDev() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isForbidden());
    }
}
