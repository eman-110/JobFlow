package com.jobflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OAuthAuthorizationRequestResolverTest {
    private static final String CHALLENGE = "a".repeat(43);
    private OAuthAuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId("google-client")
                .clientSecret("not-a-real-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/{registrationId}")
                .scope(List.of("openid", "email", "profile"))
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userNameAttributeName("sub")
                .build();
        resolver = new OAuthAuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(google), "jobflow-web");
    }

    @Test
    void requiresExpectedInitiatingClient() {
        MockHttpServletRequest request = request("another-client", CHALLENGE);
        assertThrows(OAuth2AuthorizationException.class, () -> resolver.resolve(request));
    }

    @Test
    void rejectsMissingOrMalformedPkceChallenge() {
        MockHttpServletRequest missing = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        missing.addParameter("client_id", "jobflow-web");
        assertThrows(OAuth2AuthorizationException.class, () -> resolver.resolve(missing));

        assertThrows(OAuth2AuthorizationException.class, () -> resolver.resolve(request("jobflow-web", "bad")));
    }

    @Test
    void createsProviderAuthorizationRequestWithPkceAndSessionBinding() {
        MockHttpServletRequest request = request("jobflow-web", CHALLENGE);

        var authorization = resolver.resolve(request);

        assertNotNull(authorization);
        assertNotNull(authorization.getState());
        assertNotNull(authorization.getAdditionalParameters().get("nonce"));
        assertNotNull(authorization.getAdditionalParameters().get("code_challenge"));
        assertEquals("S256", authorization.getAdditionalParameters().get("code_challenge_method"));
        assertNotNull(authorization.getAttributes().get("code_verifier"));
        assertEquals("jobflow-web", request.getSession().getAttribute(
                OAuthAuthorizationRequestResolver.CLIENT_SESSION_ATTRIBUTE));
        assertEquals(CHALLENGE, request.getSession().getAttribute(
                OAuthAuthorizationRequestResolver.CHALLENGE_SESSION_ATTRIBUTE));
    }

    @Test
    void forcesFreshProviderAuthenticationForAccountLinking() {
        MockHttpServletRequest request = request("jobflow-web", CHALLENGE);
        request.addParameter("link_token", "opaque-one-use-intent");

        var authorization = resolver.resolve(request);

        assertEquals("login", authorization.getAdditionalParameters().get("prompt"));
        assertEquals("0", authorization.getAdditionalParameters().get("max_age"));
        assertEquals("opaque-one-use-intent", request.getSession().getAttribute(
                OAuthAuthorizationRequestResolver.LINK_INTENT_SESSION_ATTRIBUTE));
    }

    private static MockHttpServletRequest request(String clientId, String challenge) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.addParameter("client_id", clientId);
        request.addParameter("code_challenge", challenge);
        return request;
    }
}
