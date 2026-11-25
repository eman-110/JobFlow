package com.jobflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                             com.jobflow.service.AuthRateLimiter rateLimiter) throws Exception {
        return http
                .securityMatcher("/api/**", "/hello")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/hello", "/api/info", "/api/csrf", "/api/jobs", "/api/jobs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/oauth/link/start")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout", "/api/auth/oauth/exchange")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/**")
                        .hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/jobs/**")
                        .hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/jobs/**")
                        .hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/applications/**")
                        .hasRole("USER")
                        .anyRequest().denyAll())
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401, "Unauthorized")))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(403, "Forbidden")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(options -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(policy -> policy.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
                .addFilterBefore(new AuthRateLimitFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain denyUnmatchedRequests(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${jobflow.cors.allowed-origins:http://localhost:3000}") String configuredOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(configuredOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Request-ID"));
        cors.setExposedHeaders(List.of("Retry-After", "Location"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object roleClaim = jwt.getClaims().get("role");
            Object tokenType = jwt.getClaims().get("token_type");
            if (!validPositiveSubject(jwt.getSubject())
                    || !"access".equals(tokenType)
                    || !(roleClaim instanceof String role)
                    || !List.of("USER", "RECRUITER", "ADMIN").contains(role)) {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("Required JWT claims are missing.");
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    private static boolean validPositiveSubject(String subject) {
        if (subject == null || !subject.matches("[0-9]+")) return false;
        try {
            return Integer.parseInt(subject) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("(dev | test) & !mysql")
    UserDetailsService developmentUsers(
            PasswordEncoder encoder,
            @Value("${jobflow.security.user.username}") String userUsername,
            @Value("${jobflow.security.user.password}") String userPassword,
            @Value("${jobflow.security.recruiter.username}") String recruiterUsername,
            @Value("${jobflow.security.recruiter.password}") String recruiterPassword,
            @Value("${jobflow.security.admin.username}") String adminUsername,
            @Value("${jobflow.security.admin.password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(userUsername).password(encoder.encode(userPassword)).roles("USER").build(),
                User.withUsername(recruiterUsername).password(encoder.encode(recruiterPassword)).roles("RECRUITER").build(),
                User.withUsername(adminUsername).password(encoder.encode(adminPassword)).roles("ADMIN").build());
    }

    @Bean
    @Profile("prod & !mysql")
    UserDetailsService unavailableProductionUsers() {
        return username -> {
            throw new UsernameNotFoundException("No production identity provider is configured.");
        };
    }
}
