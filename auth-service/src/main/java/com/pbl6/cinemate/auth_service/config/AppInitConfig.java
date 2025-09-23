package com.pbl6.cinemate.auth_service.config;

import com.pbl6.cinemate.auth_service.constant.RoleName;
import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.auth_service.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppInitConfig {
    PasswordEncoder passwordEncoder;
    AppProperties appProperties;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleService roleService) {
        log.info("Initializing application.....");
        return args -> {
            if (userRepository.findByEmail(appProperties.getAdmin().getEmail()).isEmpty()) {
                Role adminRole = roleService.findByName(RoleName.ADMIN);

                User user = User.builder()
                        .email(appProperties.getAdmin().getEmail())
                        .firstName(appProperties.getAdmin().getFirstName())
                        .isEnabled(true)
                        .lastName(appProperties.getAdmin().getLastName())
                        .password(passwordEncoder.encode(appProperties.getAdmin().getPassword()))
                        .role(adminRole)
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created with default password: admin, please change it");
            }
            log.info("Application initialization completed .....");
        };
    }
}