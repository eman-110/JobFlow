package com.jobflow.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import java.util.HashMap;
import java.util.Map;

public class OAuthAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    public static final String CLIENT_SESSION_ATTRIBUTE = "jobflow.oauth.client_id";
    public static final String CHALLENGE_SESSION_ATTRIBUTE = "jobflow.oauth.code_challenge";
    public static final String LINK_INTENT_SESSION_ATTRIBUTE = "jobflow.oauth.link_intent";
    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final String expectedClientId;

    public OAuthAuthorizationRequestResolver(ClientRegistrationRepository registrations,
                                             String expectedClientId) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        this.expectedClientId = expectedClientId;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest result = delegate.resolve(request);
        return saveInitiator(request, result);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest result = delegate.resolve(request, clientRegistrationId);
        return saveInitiator(request, result);
    }

    private OAuth2AuthorizationRequest saveInitiator(HttpServletRequest request,
                                                      OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) return null;
        String clientId = request.getParameter("client_id");
        String challenge = request.getParameter("code_challenge");
        if (!expectedClientId.equals(clientId) || !validChallenge(challenge)) {
            throw new OAuth2AuthorizationException(new OAuth2Error(
                    "invalid_request", "A registered client and PKCE challenge are required.", null));
        }
        request.getSession(true).setAttribute(CLIENT_SESSION_ATTRIBUTE, clientId);
        request.getSession(true).setAttribute(CHALLENGE_SESSION_ATTRIBUTE, challenge);
        request.getSession().removeAttribute(LINK_INTENT_SESSION_ATTRIBUTE);
        String linkIntent = request.getParameter("link_token");
        if (linkIntent != null && !linkIntent.isBlank()) {
            request.getSession().setAttribute(LINK_INTENT_SESSION_ATTRIBUTE, linkIntent);
            Map<String, Object> parameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
            parameters.put("prompt", "login");
            parameters.put("max_age", "0");
            return OAuth2AuthorizationRequest.from(authorizationRequest)
                    .additionalParameters(parameters).build();
        }
        return authorizationRequest;
    }

    private static boolean validChallenge(String challenge) {
        return challenge != null && challenge.matches("[A-Za-z0-9_-]{43}");
    }
}
