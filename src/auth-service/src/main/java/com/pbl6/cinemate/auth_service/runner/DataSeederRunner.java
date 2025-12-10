package com.pbl6.cinemate.auth_service.runner;

import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.repository.RoleRepository;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.shared.constants.SeedUUIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds initial user data for development/testing purposes.
 * Only runs when app.seed.enabled=true and the users table is empty.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Default password for all seeded users: "Password@123"
    private static final String DEFAULT_PASSWORD = "Password@123";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Users table is not empty. Skipping data seeding.");
            return;
        }

        log.info("Starting user data seeding...");

        // Use fixed UUIDs for roles (must match V9__update_role_uuids.sql)
        Role adminRole = roleRepository.findById(SeedUUIDs.Roles.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found with UUID: " + SeedUUIDs.Roles.ADMIN));
        Role userRole = roleRepository.findById(SeedUUIDs.Roles.USER)
                .orElseThrow(() -> new RuntimeException("USER role not found with UUID: " + SeedUUIDs.Roles.USER));

        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        List<User> users = createUsers(adminRole, userRole, encodedPassword);
        userRepository.saveAll(users);

        log.info("Successfully seeded {} users", users.size());
    }

    private List<User> createUsers(Role adminRole, Role userRole, String encodedPassword) {
        LocalDateTime now = LocalDateTime.now();

        return Arrays.asList(
                // Admin users (2)
                createUser(SeedUUIDs.Users.ADMIN_01, "admin1@cinemate.com", "Super", "Admin",
                        encodedPassword, adminRole, true, now),
                createUser(SeedUUIDs.Users.ADMIN_02, "admin2@cinemate.com", "System", "Admin",
                        encodedPassword, adminRole, true, now),

                // Regular users (13) - enabled
                createUser(SeedUUIDs.Users.USER_01, "user01@example.com", "John", "Doe",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_02, "user02@example.com", "Jane", "Smith",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_03, "user03@example.com", "Robert", "Johnson",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_04, "user04@example.com", "Emily", "Williams",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_05, "user05@example.com", "Michael", "Brown",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_06, "user06@example.com", "Sarah", "Davis",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_07, "user07@example.com", "David", "Miller",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_08, "user08@example.com", "Lisa", "Wilson",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_09, "user09@example.com", "James", "Moore",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_10, "user10@example.com", "Jennifer", "Taylor",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_11, "user11@example.com", "William", "Anderson",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_12, "user12@example.com", "Jessica", "Thomas",
                        encodedPassword, userRole, true, now),
                createUser(SeedUUIDs.Users.USER_13, "user13@example.com", "Daniel", "Jackson",
                        encodedPassword, userRole, true, now),

                // Disabled user (for testing)
                createUser(SeedUUIDs.Users.USER_DISABLED, "disabled@example.com", "Disabled", "User",
                        encodedPassword, userRole, false, null));
    }

    private User createUser(UUID id, String email, String firstName, String lastName,
            String password, Role role, boolean enabled, LocalDateTime verifiedAt) {
        return User.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(password)
                .role(role)
                .isEnabled(enabled)
                .accountVerifiedAt(verifiedAt)
                .build();
    }
}
