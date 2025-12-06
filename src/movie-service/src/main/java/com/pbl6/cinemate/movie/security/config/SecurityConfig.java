package com.pbl6.cinemate.movie.security.config;

import com.pbl6.cinemate.shared.security.JwtAuthEntryPoint;
import com.pbl6.cinemate.shared.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    private static final String[] PUBLIC_ENDPOINT = {
            "/api/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                // Handle unauthorized errors
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))

                // Stateless → Resource Server don't saved session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Only allow public endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINT).permitAll()
                        // Todo: secure other endpoints later
                        .anyRequest().permitAll())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
