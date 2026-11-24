package com.jobflow.service;

import com.jobflow.persistence.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long accessTokenMinutes;

    public JwtService(JwtEncoder encoder,
                      @Value("${jobflow.jwt.issuer}") String issuer,
                      @Value("${jobflow.jwt.audience}") String audience,
                      @Value("${jobflow.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(String subject, UserRole role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(java.util.List.of(audience))
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
                .claim("token_type", "access")
                .claim("role", role.name())
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .keyId("jobflow-hs256")
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
