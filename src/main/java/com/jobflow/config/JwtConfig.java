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
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        JwtClaimValidator<java.util.List<String>> audienceValidator =
                new JwtClaimValidator<>("aud", audiences ->
                        audiences != null && audiences.contains(audience));
        JwtClaimValidator<String> subjectValidator =
                new JwtClaimValidator<>("sub", subject -> subject != null && !subject.isBlank());
        JwtClaimValidator<String> typeValidator =
                new JwtClaimValidator<>("token_type", type -> "access".equals(type));
        JwtClaimValidator<String> roleValidator =
                new JwtClaimValidator<>("role", role ->
                        java.util.List.of("USER", "RECRUITER", "ADMIN").contains(role));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                audienceValidator, subjectValidator, typeValidator, roleValidator));
        return decoder;
    }

    private byte[] resolveSecret(String configuredSecret, Environment environment) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(configuredSecret);
                if (decoded.length < 32) {
                    throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes.");
                }
                return decoded;
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("JWT_SECRET must be valid Base64.", exception);
            }
        }
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException("JWT_SECRET is required in the prod profile.");
        }
        return "JobFlow-local-development-secret-32bytes!".getBytes(StandardCharsets.UTF_8);
    }
}
