package com.logmonitor.config;

import com.logmonitor.entity.Role;
import com.logmonitor.entity.RoleName;
import com.logmonitor.entity.User;
import com.logmonitor.repository.RoleRepository;
import com.logmonitor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a default admin user in local and dev profiles when no users exist.
 *
 * <p>Default credentials: {@code admin} / {@code Admin@123} — change immediately after first login.</p>
 */
@Component
@Profile({"local", "dev"})
@Order(100)
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "Admin@123";
    private static final String DEFAULT_EMAIL = "admin@logmonitor.local";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserSeeder(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.findByRoleName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found — run Flyway migrations"));

        User admin = new User();
        admin.setUsername(DEFAULT_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setEmail(DEFAULT_EMAIL);
        admin.setEnabled(true);
        admin.addRole(adminRole);

        userRepository.save(admin);
        log.warn("Default admin user '{}' created — change the password immediately", DEFAULT_USERNAME);
    }
}
