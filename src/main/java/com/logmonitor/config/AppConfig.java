package com.logmonitor.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide configuration enabling typed property binding.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, SshProperties.class, EncryptionProperties.class})
public class AppConfig {
}
