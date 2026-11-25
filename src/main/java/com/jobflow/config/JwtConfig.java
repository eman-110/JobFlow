package com.jobflow.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class JwtConfig {
    @Bean
    JwtEncoder jwtEncoder(@Value("${jobflow.jwt.secret}") String configuredSecret,
                          Environment environment) throws JOSEException {
        byte[] secret = resolveSecret(configuredSecret, environment);
        OctetSequenceKey key = new OctetSequenceKey.Builder(secret)
                .keyID("jobflow-hs256")
                .algorithm(JWSAlgorithm.HS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(key));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${jobflow.jwt.secret}") String configuredSecret,
                          Environment environment,
                          @Value("${jobflow.jwt.issuer}") String issuer,
                          @Value("${jobflow.jwt.audience}") String audience) {
        byte[] secret = resolveSecret(configuredSecret, environment);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> requiredClaimsValidator = jwt -> {
            Object role = jwt.getClaims().get("role");
            if (jwt.getExpiresAt() == null || !validSubject(jwt.getSubject())
                    || !(role instanceof String roleName)
                    || !List.of("USER", "RECRUITER", "ADMIN").contains(roleName)
                    || !"access".equals(jwt.getClaims().get("token_type"))
                    || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required JWT claims are missing or invalid.", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), requiredClaimsValidator));
        return decoder;
    }

    private static boolean validSubject(String subject) {
        if (subject == null || !subject.matches("[0-9]+")) return false;
        try {
            return Integer.parseInt(subject) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    byte[] resolveSecret(String configuredSecret, Environment environment) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(configuredSecret);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("JWT_SECRET must be valid Base64.", exception);
            }
            if (decoded.length < 32) {
                throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes.");
            }
            return decoded;
        }
        if (environment.matchesProfiles("test")) {
            throw new IllegalStateException("The test profile must configure an explicit JWT secret.");
        }
        throw new IllegalStateException("JWT_SECRET is required outside the test profile.");
    }
}
