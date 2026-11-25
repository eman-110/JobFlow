package com.jobflow.service;

import com.jobflow.persistence.entity.ExternalIdentityEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.entity.UserRole;
import com.jobflow.persistence.repository.ExternalIdentityJpaRepository;
import com.jobflow.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
@Profile("mysql")
public class ExternalLoginService {
    private final ExternalIdentityJpaRepository identities;
    private final UserJpaRepository users;
    private final String expectedIssuer;
    private final String expectedAudience;

    public ExternalLoginService(ExternalIdentityJpaRepository identities, UserJpaRepository users,
                                @Value("${spring.security.oauth2.client.provider.google.issuer-uri:https://accounts.google.com}")
                                String expectedIssuer,
                                @Value("${spring.security.oauth2.client.registration.google.client-id:}")
                                String expectedAudience) {
        this.identities = identities;
        this.users = users;
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
    }

    @Transactional
    public UserEntity resolveOrCreate(OidcUser principal) {
        IdentityClaims claims = claims(principal);
        return identities.findByIssuerAndSubject(claims.issuer(), claims.subject())
                .map(ExternalIdentityEntity::getUser)
                .orElseGet(() -> createAccount(claims));
    }

    @Transactional
    void linkToAuthenticatedAccount(Integer authenticatedUserId, Integer targetUserId,
                                    OidcUser principal, Instant authenticatedAt) {
        if (authenticatedUserId == null || !Objects.equals(authenticatedUserId, targetUserId)
                || authenticatedUserId <= 0) {
            throw oauthFailure("Account ownership could not be established.");
        }
        if (authenticatedAt == null || authenticatedAt.isBefore(Instant.now().minusSeconds(300))
                || authenticatedAt.isAfter(Instant.now().plusSeconds(30))) {
            throw oauthFailure("Fresh provider authentication is required.");
        }
        IdentityClaims claims = claims(principal);
        if (!claims.emailVerified()) {
            throw oauthFailure("A verified provider email is required to link an identity.");
        }
        UserEntity target = users.findById(authenticatedUserId)
                .orElseThrow(() -> oauthFailure("The local account does not exist."));
        var existing = identities.findByIssuerAndSubjectForUpdate(claims.issuer(), claims.subject());
        if (existing.isPresent()) {
            if (!existing.get().getUser().getId().equals(authenticatedUserId)) {
                throw oauthFailure("This external identity is already linked to another account.");
            }
            return;
        }
        identities.saveAndFlush(new ExternalIdentityEntity(target, claims.issuer(), claims.subject()));
    }

    private UserEntity createAccount(IdentityClaims claims) {
        if (!claims.emailVerified() || claims.email() == null || claims.email().isBlank()
                || claims.email().trim().length() > 255) {
            throw oauthFailure("A verified email address is required to create an account.");
        }
        String email = claims.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw oauthFailure("An account already uses this email; sign in and link it explicitly.");
        }
        String name = claims.name() == null || claims.name().isBlank() ? email : claims.name().trim();
        if (name.length() > 100) name = name.substring(0, 100);
        UserEntity user = new UserEntity(name, email);
        user.setRole(UserRole.USER);
        user.setPasswordHash(null);
        user = users.saveAndFlush(user);
        identities.saveAndFlush(new ExternalIdentityEntity(user, claims.issuer(), claims.subject()));
        return user;
    }

    private IdentityClaims claims(OidcUser principal) {
        if (principal == null || principal.getIdToken() == null
                || principal.getIdToken().getIssuer() == null
                || !expectedIssuer.equals(principal.getIdToken().getIssuer().toString())
                || expectedAudience.isBlank()
                || principal.getIdToken().getAudience() == null
                || !principal.getIdToken().getAudience().contains(expectedAudience)
                || principal.getSubject() == null
                || principal.getSubject().isBlank()) {
            throw oauthFailure("The provider identity is incomplete.");
        }
        Object verified = principal.getClaims().get("email_verified");
        return new IdentityClaims(principal.getIdToken().getIssuer().toString(), principal.getSubject(),
                principal.getClaimAsString("email"), principal.getClaimAsString("name"),
                Boolean.TRUE.equals(verified) || "true".equals(verified));
    }

    private static OAuth2AuthenticationException oauthFailure(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), message);
    }

    private record IdentityClaims(String issuer, String subject, String email, String name,
                                  boolean emailVerified) {}
}
