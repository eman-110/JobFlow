package com.jobflow.service;

import com.jobflow.config.OAuthAuthorizationRequestResolver;
import com.jobflow.persistence.entity.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthLoginSuccessHandlerTest {
    private ExternalLoginService externalLogins;
    private OAuthLoginExchangeService exchanges;
    private OAuthLinkIntentService linkIntents;
    private OAuthLoginSuccessHandler handler;
    private OidcUser principal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        externalLogins = mock(ExternalLoginService.class);
        exchanges = mock(OAuthLoginExchangeService.class);
        linkIntents = mock(OAuthLinkIntentService.class);
        handler = new OAuthLoginSuccessHandler(externalLogins, exchanges, linkIntents,
                "https://app.example.test/auth/callback", "production");
        principal = mock(OidcUser.class);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    void redirectsWithOnlyShortLivedExchangeCodeAndClearsInitiationData() throws Exception {
        MockHttpServletRequest request = request();
        when(externalLogins.resolveOrCreate(principal))
                .thenReturn(new UserEntity("OAuth user", "oauth@example.test"));
        when(exchanges.issue(any(), eq("jobflow-web"), eq("a".repeat(43)))).thenReturn("opaque-exchange-code");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
        assertEquals("https://app.example.test/auth/callback?code=opaque-exchange-code",
                response.getRedirectedUrl());
        assertFalse(response.getRedirectedUrl().contains("accessToken"));
        assertFalse(response.getRedirectedUrl().contains("refreshToken"));
        assertNull(request.getSession().getAttribute(OAuthAuthorizationRequestResolver.CLIENT_SESSION_ATTRIBUTE));
        assertNull(request.getSession().getAttribute(OAuthAuthorizationRequestResolver.CHALLENGE_SESSION_ATTRIBUTE));
    }

    @Test
    void rejectsCallbackWithoutSavedAuthorizationContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        verifyNoInteractions(externalLogins, exchanges);
    }

    @Test
    void rejectsNonOidcPrincipalWithoutIssuingExchangeCredential() throws Exception {
        MockHttpServletRequest request = request();
        when(authentication.getPrincipal()).thenReturn("not-an-oidc-principal");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verifyNoInteractions(externalLogins, exchanges);
    }

    @Test
    void linksOnlyWhenOneUseIntentAndFreshProviderAuthenticationArePresent() throws Exception {
        MockHttpServletRequest request = request();
        request.getSession().setAttribute(OAuthAuthorizationRequestResolver.LINK_INTENT_SESSION_ATTRIBUTE, "opaque-link-intent");
        OidcIdToken idToken = mock(OidcIdToken.class);
        Instant authenticatedAt = Instant.now();
        when(principal.getIdToken()).thenReturn(idToken);
        when(idToken.getClaimAsInstant("auth_time")).thenReturn(authenticatedAt);
        when(linkIntents.consume("opaque-link-intent", "jobflow-web", "a".repeat(43))).thenReturn(31);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(externalLogins).linkToAuthenticatedAccount(31, 31, principal, authenticatedAt);
        assertEquals("https://app.example.test/auth/callback?linked=true", response.getRedirectedUrl());
        verifyNoInteractions(exchanges);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(OAuthAuthorizationRequestResolver.CLIENT_SESSION_ATTRIBUTE, "jobflow-web");
        session.setAttribute(OAuthAuthorizationRequestResolver.CHALLENGE_SESSION_ATTRIBUTE, "a".repeat(43));
        request.setSession(session);
        return request;
    }
}
