package com.jobflow.service;

import com.jobflow.persistence.entity.ExternalIdentityEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.entity.UserRole;
import com.jobflow.persistence.repository.ExternalIdentityJpaRepository;
import com.jobflow.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExternalLoginServiceTest {
    private ExternalIdentityJpaRepository identities;
    private UserJpaRepository users;
    private ExternalLoginService service;

    @BeforeEach
    void setUp() {
        identities = mock(ExternalIdentityJpaRepository.class);
        users = mock(UserJpaRepository.class);
        service = new ExternalLoginService(identities, users,
                "https://accounts.google.com", "google-client");
    }

    @Test
    void resolvesKnownIssuerAndSubjectWithoutMatchingEmail() {
        UserEntity existing = user(11, "existing@example.test");
        when(identities.findByIssuerAndSubject("https://accounts.google.com", "subject-1"))
                .thenReturn(Optional.of(new ExternalIdentityEntity(existing,
                        "https://accounts.google.com", "subject-1")));
        OidcUser principal = oidc("subject-1", "different@example.test", true, Map.of());

        assertSame(existing, service.resolveOrCreate(principal));
        verifyNoInteractions(users);
    }

    @Test
    void createsVerifiedExternalOnlyAccountWithUserRoleAndNoPassword() {
        when(users.findByEmailIgnoreCase("new@example.test")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = service.resolveOrCreate(oidc("subject-2", "NEW@example.test", true, Map.of("role", "ADMIN")));

        assertEquals("new@example.test", result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        assertNull(result.getPasswordHash());
        verify(identities).saveAndFlush(argThat(identity ->
                identity.getIssuer().equals("https://accounts.google.com")
                        && identity.getSubject().equals("subject-2")));
    }

    @Test
    void normalizesNameAndEmailWhenCreatingLocalAccount() {
        when(users.findByEmailIgnoreCase("person@example.test")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = service.resolveOrCreate(oidc("subject-3", " Person@Example.Test ", true,
                Map.of("name", "  Example Person  ")));

        assertEquals("person@example.test", result.getEmail());
        assertEquals("Example Person", result.getName());
    }

    @Test
    void rejectsUnverifiedEmailForAutomaticAccountCreation() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolveOrCreate(oidc("subject-4", "person@example.test", false, Map.of())));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingEmailForAutomaticAccountCreation() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolveOrCreate(oidc("subject-5", null, true, Map.of())));
    }

    @Test
    void refusesToAutomaticallyLinkMatchingLocalEmail() {
        when(users.findByEmailIgnoreCase("person@example.test"))
                .thenReturn(Optional.of(user(12, "person@example.test")));

        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolveOrCreate(oidc("subject-6", "person@example.test", true, Map.of())));
        verify(identities, never()).saveAndFlush(any());
    }

    @Test
    void rejectsIncorrectIssuer() {
        OidcUser principal = oidc("subject-7", "person@example.test", true,
                Map.of("iss", "https://attacker.example"));
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveOrCreate(principal));
    }

    @Test
    void rejectsIncorrectAudience() {
        OidcUser principal = oidc("subject-8", "person@example.test", true,
                Map.of("aud", List.of("another-client")));
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveOrCreate(principal));
    }

    @Test
    void rejectsBlankSubject() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolveOrCreate(oidc(" ", "person@example.test", true, Map.of())));
    }

    @Test
    void rejectsMissingOidcIdToken() {
        OidcUser principal = mock(OidcUser.class);
        when(principal.getIdToken()).thenReturn(null);
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveOrCreate(principal));
    }

    @Test
    void ignoresAdministrativeClaimsOnNewAccounts() {
        when(users.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.empty());
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UserEntity result = service.resolveOrCreate(oidc("subject-9", "admin@example.test", true,
                Map.of("role", "ADMIN", "groups", List.of("administrators"))));

        assertEquals(UserRole.USER, result.getRole());
    }

    @Test
    void rejectsLinkWhenAuthenticatedAndTargetUserDiffer() {
        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 16,
                        oidc("subject-10", "person@example.test", true, Map.of()), Instant.now()));
    }

    @Test
    void rejectsStaleProviderAuthenticationForLinking() {
        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 15,
                        oidc("subject-11", "person@example.test", true, Map.of()),
                        Instant.now().minusSeconds(301)));
    }

    @Test
    void rejectsFutureProviderAuthenticationForLinking() {
        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 15,
                        oidc("subject-12", "person@example.test", true, Map.of()),
                        Instant.now().plusSeconds(31)));
    }

    @Test
    void rejectsLinkWhenProviderEmailIsUnverified() {
        when(users.findById(15)).thenReturn(Optional.of(user(15, "local@example.test")));
        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 15,
                        oidc("subject-13", "person@example.test", false, Map.of()), Instant.now()));
    }

    @Test
    void rejectsIdentityAlreadyOwnedByAnotherUser() {
        UserEntity other = user(19, "other@example.test");
        when(identities.findByIssuerAndSubjectForUpdate("https://accounts.google.com", "subject-14"))
                .thenReturn(Optional.of(new ExternalIdentityEntity(other,
                        "https://accounts.google.com", "subject-14")));

        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 15,
                        oidc("subject-14", "person@example.test", true, Map.of()), Instant.now()));
    }

    @Test
    void linksOnlyToTheAuthenticatedExistingLocalAccount() {
        UserEntity target = user(15, "local@example.test");
        when(users.findById(15)).thenReturn(Optional.of(target));
        when(identities.findByIssuerAndSubjectForUpdate("https://accounts.google.com", "subject-15"))
                .thenReturn(Optional.empty());
        when(identities.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.linkToAuthenticatedAccount(15, 15,
                oidc("subject-15", "verified@example.test", true, Map.of()), Instant.now()));
        verify(identities).saveAndFlush(argThat(identity -> identity.getUser().getId().equals(15)));
    }

    @Test
    void linkingSameExistingIdentityIsIdempotent() {
        UserEntity target = user(15, "local@example.test");
        when(users.findById(15)).thenReturn(Optional.of(target));
        when(identities.findByIssuerAndSubjectForUpdate("https://accounts.google.com", "subject-16"))
                .thenReturn(Optional.of(new ExternalIdentityEntity(target,
                        "https://accounts.google.com", "subject-16")));

        assertDoesNotThrow(() -> service.linkToAuthenticatedAccount(15, 15,
                oidc("subject-16", "verified@example.test", true, Map.of()), Instant.now()));
        verify(identities, never()).saveAndFlush(any());
    }

    @Test
    void refusesLinkIfLocalAccountNoLongerExists() {
        when(users.findById(15)).thenReturn(Optional.empty());
        assertThrows(OAuth2AuthenticationException.class, () ->
                service.linkToAuthenticatedAccount(15, 15,
                        oidc("subject-17", "verified@example.test", true, Map.of()), Instant.now()));
    }

    private static UserEntity user(int id, String email) {
        UserEntity user = new UserEntity("Local User", email);
        user.setId(id);
        return user;
    }

    private static OidcUser oidc(String subject, String email, boolean verified, Map<String, Object> overrides) {
        Instant now = Instant.now();
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("iss", overrides.getOrDefault("iss", "https://accounts.google.com"));
        claims.put("sub", subject);
        claims.put("aud", overrides.getOrDefault("aud", List.of("google-client")));
        claims.put("iat", now);
        claims.put("exp", now.plusSeconds(600));
        if (email != null) claims.put("email", email);
        claims.put("email_verified", verified);
        claims.put("name", overrides.getOrDefault("name", "OIDC User"));
        claims.putAll(overrides);
        OidcIdToken token = new OidcIdToken("opaque-id-token", now, now.plusSeconds(600), claims);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), token, "sub");
    }
}
