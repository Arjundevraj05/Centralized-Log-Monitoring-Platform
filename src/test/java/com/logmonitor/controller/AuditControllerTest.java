package com.logmonitor.controller;

import com.logmonitor.audit.AuditService;
import com.logmonitor.entity.AuditLog;
import com.logmonitor.exception.GlobalExceptionHandler;
import com.logmonitor.mapper.AuditLogMapper;
import com.logmonitor.security.CustomUserDetailsService;
import com.logmonitor.security.JwtAccessDeniedHandler;
import com.logmonitor.security.JwtAuthenticationEntryPoint;
import com.logmonitor.security.JwtAuthenticationFilter;
import com.logmonitor.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link AuditController}.
 */
@WebMvcTest(controllers = AuditController.class)
@Import({GlobalExceptionHandler.class, AuditLogMapper.class, SecurityConfig.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogs_returnsPage() throws Exception {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setUsername("admin");
        auditLog.setAction("USER_LOGIN");
        auditLog.setResource("admin");
        auditLog.setTimestamp(Instant.now());

        when(auditService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLog)));

        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin"));
    }

    @Test
    @WithMockUser(roles = "DEV")
    void getAuditLogs_deniedForDev() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isForbidden());
    }
}
