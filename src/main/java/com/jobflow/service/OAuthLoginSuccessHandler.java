package com.jobflow.service;

import com.jobflow.config.OAuthAuthorizationRequestResolver;
import com.jobflow.persistence.entity.UserEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Component
@Profile("mysql")
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);
    private final ExternalLoginService externalLogins;
    private final OAuthLoginExchangeService exchanges;
    private final OAuthLinkIntentService linkIntents;
    private final String successRedirect;

    public OAuthLoginSuccessHandler(ExternalLoginService externalLogins, OAuthLoginExchangeService exchanges,
                                    OAuthLinkIntentService linkIntents,
                                    @Value("${jobflow.oauth.success-redirect:http://localhost:3000/auth/callback}")
                                    String successRedirect,
                                    @Value("${app.environment:development}") String environment) {
        URI redirect = URI.create(successRedirect);
        boolean localHttp = "http".equalsIgnoreCase(redirect.getScheme())
                && ("localhost".equalsIgnoreCase(redirect.getHost())
                || "127.0.0.1".equals(redirect.getHost()));
        if (redirect.getHost() == null || redirect.getRawUserInfo() != null
                || redirect.getRawFragment() != null
                || !("https".equalsIgnoreCase(redirect.getScheme()) || localHttp)
                || "production".equalsIgnoreCase(environment) && !"https".equalsIgnoreCase(redirect.getScheme())) {
            throw new IllegalArgumentException("OAuth success redirect must be a trusted HTTPS URI.");
        }
        this.externalLogins = externalLogins;
        this.exchanges = exchanges;
        this.linkIntents = linkIntents;
        this.successRedirect = successRedirect;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser oidcUser) || request.getSession(false) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth authentication failed.");
            return;
        }
        Object client = request.getSession().getAttribute(OAuthAuthorizationRequestResolver.CLIENT_SESSION_ATTRIBUTE);
        Object challenge = request.getSession().getAttribute(OAuthAuthorizationRequestResolver.CHALLENGE_SESSION_ATTRIBUTE);
        Object linkIntent = request.getSession().getAttribute(OAuthAuthorizationRequestResolver.LINK_INTENT_SESSION_ATTRIBUTE);
        request.getSession().removeAttribute(OAuthAuthorizationRequestResolver.CLIENT_SESSION_ATTRIBUTE);
        request.getSession().removeAttribute(OAuthAuthorizationRequestResolver.CHALLENGE_SESSION_ATTRIBUTE);
        request.getSession().removeAttribute(OAuthAuthorizationRequestResolver.LINK_INTENT_SESSION_ATTRIBUTE);
        if (!(client instanceof String clientId) || !(challenge instanceof String codeChallenge)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth login initiation is missing.");
            return;
        }
        try {
            if (linkIntent instanceof String secret) {
                Integer userId = linkIntents.consume(secret, clientId, codeChallenge);
                externalLogins.linkToAuthenticatedAccount(userId, userId, oidcUser,
                        oidcUser.getIdToken().getClaimAsInstant("auth_time"));
                log.info("audit event=oauth_identity_link_success provider=oidc");
                String linkedLocation = UriComponentsBuilder.fromUriString(successRedirect)
                        .queryParam("linked", "true").build().encode().toUriString();
                response.sendRedirect(linkedLocation);
                return;
            }
            UserEntity user = externalLogins.resolveOrCreate(oidcUser);
            String exchange = exchanges.issue(user, clientId, codeChallenge);
            log.info("audit event=oauth_login_success provider=oidc");
            String location = UriComponentsBuilder.fromUriString(successRedirect)
                    .queryParam("code", exchange)
                    .build().encode().toUriString();
            response.sendRedirect(location);
        } catch (OAuth2AuthenticationException | OAuthLoginExchangeService.ExchangeException exception) {
            log.warn("audit event=oauth_login_rejected provider=oidc");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "External authentication failed.");
        }
    }
}
