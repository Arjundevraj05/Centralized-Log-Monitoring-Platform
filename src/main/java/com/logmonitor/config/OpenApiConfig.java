package com.logmonitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration with JWT bearer security scheme.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Configures the OpenAPI document with JWT authentication support.
     *
     * @return OpenAPI bean
     */
    @Bean
    OpenAPI logMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Centralized Log Monitoring Platform API")
                        .description("Secure log fetching, searching, and streaming from remote servers via SSH. "
                                + "Real-time streaming: connect to WebSocket endpoint `/ws`, subscribe to `/topic/logs`, "
                                + "send STOMP messages to `/app/logs/stream/start` and `/app/logs/stream/stop`. "
                                + "Pass JWT via STOMP CONNECT `Authorization: Bearer <token>` header.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Log Monitor Team")
                                .email("admin@logmonitor.local")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from POST /api/auth/login")));
    }
}
