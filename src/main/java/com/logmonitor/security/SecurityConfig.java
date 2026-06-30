package com.logmonitor.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration with JWT authentication and role-based access control.
 *
 * <p>RBAC matrix:</p>
 * <ul>
 *   <li>ADMIN — manage users/servers, view/search logs, audit trail</li>
 *   <li>DEV — view and search logs</li>
 *   <li>SUPPORT — view logs only</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler,
                          CustomUserDetailsService userDetailsService,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Configures the stateless JWT security filter chain.
     *
     * @param http the HTTP security builder
     * @return configured filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Server management — read for all roles, write for ADMIN only
                        .requestMatchers(HttpMethod.GET, "/api/servers", "/api/servers/**")
                                .hasAnyRole("ADMIN", "DEV", "SUPPORT")
                        .requestMatchers("/api/servers", "/api/servers/**")
                                .hasRole("ADMIN")

                        // Log operations — SUPPORT cannot search
                        .requestMatchers(HttpMethod.POST, "/api/logs/search")
                                .hasAnyRole("ADMIN", "DEV")
                        .requestMatchers("/api/logs/**", "/api/log-types", "/api/log-types/**",
                                "/api/app-logs/**")
                                .hasAnyRole("ADMIN", "DEV", "SUPPORT")

                        // Tomcat discovery on servers
                        .requestMatchers(HttpMethod.GET, "/api/servers/*/tomcat/**")
                                .hasAnyRole("ADMIN", "DEV", "SUPPORT")
                        .requestMatchers("/api/servers/*/tomcat/**")
                                .hasAnyRole("ADMIN", "DEV")

                        // Audit trail — ADMIN only
                        .requestMatchers("/api/audit", "/api/audit/**")
                                .hasRole("ADMIN")

                        // User management — ADMIN only
                        .requestMatchers("/api/users", "/api/users/**")
                                .hasRole("ADMIN")

                        // WebSocket — JWT validated on STOMP CONNECT frame
                        .requestMatchers("/ws/**")
                                .permitAll()

                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DAO authentication provider using BCrypt password verification.
     *
     * @return configured authentication provider
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the Spring {@link AuthenticationManager} for login authentication.
     *
     * @param configuration authentication configuration
     * @return authentication manager
     * @throws Exception if manager cannot be created
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder with strength 12.
     *
     * @return password encoder bean
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
