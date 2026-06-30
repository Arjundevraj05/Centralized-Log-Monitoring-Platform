package com.logmonitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logmonitor.dto.LogFetchRequest;
import com.logmonitor.dto.LogResponse;
import com.logmonitor.dto.LogSearchRequest;
import com.logmonitor.dto.LogTypeResponse;
import com.logmonitor.config.CorsConfig;
import com.logmonitor.exception.GlobalExceptionHandler;
import com.logmonitor.security.CustomUserDetailsService;
import com.logmonitor.security.JwtAccessDeniedHandler;
import com.logmonitor.security.JwtAuthenticationEntryPoint;
import com.logmonitor.security.JwtAuthenticationFilter;
import com.logmonitor.security.SecurityConfig;
import com.logmonitor.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link LogController}.
 */
@WebMvcTest(controllers = LogController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogService logService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "DEV")
    void getLogTypes_returnsTypes() throws Exception {
        when(logService.getLogTypes()).thenReturn(
                List.of(new LogTypeResponse("Tomcat Log", "TOMCAT_LOG"))
        );

        mockMvc.perform(get("/api/log-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commandKey").value("TOMCAT_LOG"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    void fetchLogs_returnsLogLines() throws Exception {
        when(logService.fetchLogs(any())).thenReturn(
                new LogResponse(1L, "TOMCAT_LOG", List.of("log line"))
        );

        LogFetchRequest request = new LogFetchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");

        mockMvc.perform(post("/api/logs/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCount").value(1));
    }

    @Test
    @WithMockUser(roles = "DEV")
    void searchLogs_allowedForDev() throws Exception {
        when(logService.searchLogs(any())).thenReturn(
                new LogResponse(1L, "TOMCAT_LOG", List.of("error line"))
        );

        LogSearchRequest request = new LogSearchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");
        request.setSearchTerm("ERROR");

        mockMvc.perform(post("/api/logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    void searchLogs_deniedForSupport() throws Exception {
        LogSearchRequest request = new LogSearchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");
        request.setSearchTerm("ERROR");

        mockMvc.perform(post("/api/logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
