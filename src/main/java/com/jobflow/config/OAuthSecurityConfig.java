package com.jobflow.config;

import com.jobflow.service.OAuthLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("oauth & mysql")
public class OAuthSecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(OAuthSecurityConfig.class);

    @Bean
    @Order(1)
    SecurityFilterChain browserOAuthChain(
            HttpSecurity http,
            ClientRegistrationRepository registrations,
            OAuthLoginSuccessHandler successHandler,
            @Value("${jobflow.oauth.app-client-id}") String appClientId) throws Exception {
        return http
                .securityMatcher("/oauth2/**", "/login/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        new OAuthAuthorizationRequestResolver(registrations, appClientId)))
                                .successHandler(successHandler)
                                .failureHandler((request, response, exception) -> {
                                    log.warn("audit event=oauth_authentication_failure");
                                    response.sendError(401, "External authentication failed.");
                                }))
                .headers(headers -> headers
                        .contentTypeOptions(options -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(policy -> policy.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'")))
                .build();
    }
}
