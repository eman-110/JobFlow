package com.jobflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET,
                                "/hello", "/api/info", "/api/csrf", "/api/jobs", "/api/jobs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout")
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
                // This API uses bearer credentials only. It has no cookie/session auth,
                // so CSRF tokens are neither issued nor required.
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401, "Unauthorized")))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(403, "Forbidden")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || !"access".equals(jwt.getClaimAsString("token_type"))
                    || jwt.getClaimAsString("role") == null) {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("Required JWT claims are missing.");
            }
            String role = jwt.getClaimAsString("role");
            if (!List.of("USER", "RECRUITER", "ADMIN").contains(role)) {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("JWT role is invalid.");
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
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
