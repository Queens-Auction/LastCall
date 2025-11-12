package org.example.lastcall.common.security;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/auctions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/email-verifications/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/email-verifications/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me/password").permitAll()
                        .requestMatchers("/actuator/**").permitAll() // <-- 필수
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
