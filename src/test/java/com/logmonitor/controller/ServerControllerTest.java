package com.logmonitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logmonitor.dto.ServerRequest;
import com.logmonitor.dto.ServerResponse;
import com.logmonitor.exception.GlobalExceptionHandler;
import com.logmonitor.security.CustomUserDetailsService;
import com.logmonitor.security.JwtAccessDeniedHandler;
import com.logmonitor.security.JwtAuthenticationEntryPoint;
import com.logmonitor.security.JwtAuthenticationFilter;
import com.logmonitor.security.SecurityConfig;
import com.logmonitor.service.ServerService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link ServerController}.
 */
@WebMvcTest(controllers = ServerController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class ServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServerService serverService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllServers_returnsList() throws Exception {
        ServerResponse server = new ServerResponse();
        server.setId(1L);
        server.setServerName("prod-01");
        when(serverService.findAll()).thenReturn(List.of(server));

        mockMvc.perform(get("/api/servers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serverName").value("prod-01"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createServer_returnsCreated() throws Exception {
        ServerRequest request = new ServerRequest();
        request.setServerName("prod-01");
        request.setHost("10.0.0.1");
        request.setPort(22);
        request.setUsername("deploy");
        request.setPrivateKey("pem-key");
        request.setEnvironment("prod");
        request.setActive(true);

        ServerResponse response = new ServerResponse();
        response.setId(1L);
        response.setServerName("prod-01");
        when(serverService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteServer_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/servers/1"))
                .andExpect(status().isNoContent());

        verify(serverService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "SUPPORT")
    void createServer_deniedForSupport() throws Exception {
        mockMvc.perform(post("/api/servers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateServer_returnsOk() throws Exception {
        ServerRequest request = new ServerRequest();
        request.setServerName("prod-01");
        request.setHost("10.0.0.1");
        request.setPort(22);
        request.setUsername("deploy");
        request.setEnvironment("prod");
        request.setActive(true);

        when(serverService.update(eq(1L), any())).thenReturn(new ServerResponse());

        mockMvc.perform(put("/api/servers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
