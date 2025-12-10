package com.pbl6.microservices.customer_service.security.config;

import com.pbl6.microservices.customer_service.constants.ApiPath;
import com.pbl6.microservices.customer_service.security.entrypoint.JwtAuthEntryPoint;
import com.pbl6.microservices.customer_service.security.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthFilter jwtAuthFilter;
        private final JwtAuthEntryPoint jwtAuthEntryPoint;

        public final String[] PUBLIC_ENDPOINT = {
                        ApiPath.UPDATE_PROFILE
        };

        // Internal endpoints accessible only within Docker network
        public final String[] INTERNAL_ENDPOINT = {
                        "/internal/**"
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                return http.csrf(AbstractHttpConfigurer::disable)
                                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthEntryPoint))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PUBLIC_ENDPOINT).permitAll()
                                                .requestMatchers(INTERNAL_ENDPOINT).permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

}
