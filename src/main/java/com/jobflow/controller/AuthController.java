package com.jobflow.controller;

import com.jobflow.api.auth.LoginRequest;
import com.jobflow.api.auth.OAuthExchangeRequest;
import com.jobflow.api.auth.OAuthLinkStartRequest;
import com.jobflow.api.auth.OAuthLinkStartResponse;
import com.jobflow.api.auth.RefreshRequest;
import com.jobflow.api.auth.RegisterRequest;
import com.jobflow.api.auth.TokenPairResponse;
import com.jobflow.api.auth.UserResponse;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.entity.UserRole;
import com.jobflow.persistence.repository.UserJpaRepository;
import com.jobflow.service.JwtService;
import com.jobflow.service.RefreshTokenService;
import com.jobflow.service.OAuthLoginExchangeService;
import com.jobflow.service.OAuthLinkIntentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtService jwtService;
    private final Optional<UserJpaRepository> users;
    private final Optional<RefreshTokenService> refreshTokens;
    private final Optional<OAuthLoginExchangeService> oauthExchanges;
    private final Optional<OAuthLinkIntentService> oauthLinkIntents;
    private final String oauthAppClientId;
    private final String oauthAuthorizationBaseUrl;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final long accessTokenMinutes;

    @Autowired
    public AuthController(JwtService jwtService,
                          @Autowired(required = false) UserJpaRepository users,
                          @Autowired(required = false) RefreshTokenService refreshTokens,
                          @Autowired(required = false) OAuthLoginExchangeService oauthExchanges,
                          @Autowired(required = false) OAuthLinkIntentService oauthLinkIntents,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          @Value("${jobflow.oauth.app-client-id:jobflow-web}") String oauthAppClientId,
                          @Value("${jobflow.oauth.authorization-base-url:http://localhost:8080}")
                          String oauthAuthorizationBaseUrl,
                          @Value("${jobflow.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.jwtService = jwtService;
        this.users = Optional.ofNullable(users);
        this.refreshTokens = Optional.ofNullable(refreshTokens);
        this.oauthExchanges = Optional.ofNullable(oauthExchanges);
        this.oauthLinkIntents = Optional.ofNullable(oauthLinkIntents);
        this.oauthAppClientId = oauthAppClientId;
        this.oauthAuthorizationBaseUrl = oauthAuthorizationBaseUrl;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (users.isEmpty()) {
            throw new IllegalStateException("Registration requires a database-backed profile.");
        }
        String email = normalizeEmail(request.email());
        validatePassword(request.password());
        if (users.get().findByEmailIgnoreCase(email).isPresent()) {
            log.warn("audit event=registration_rejected reason=duplicate_email");
            throw new DuplicateEmailException("An account with that email already exists.");
        }
        UserEntity user = new UserEntity(request.name().trim(), email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        try {
            user = users.get().saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            log.warn("audit event=registration_rejected reason=duplicate_email");
            throw new DuplicateEmailException("An account with that email already exists.");
        }
        log.info("audit event=registration_success method=password");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getName(), user.getEmail(), "USER"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (users.isEmpty()) {
            log.warn("audit event=login_failure method=password");
            throw new BadCredentialsException("Invalid email or password.");
        }
        UserEntity user = users.get().findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            log.warn("audit event=login_failure method=password");
            throw new BadCredentialsException("Invalid email or password.");
        }
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("audit event=login_failure method=password");
            throw new BadCredentialsException("Invalid email or password.");
        }
        log.info("audit event=login_success method=password");
        return ResponseEntity.ok(issuePair(user, user.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService service = refreshTokens.orElseThrow(
                () -> new RefreshTokenService.RefreshTokenException("Refresh credentials are unavailable."));
        RefreshTokenService.RotatedUser rotated = service.rotate(request.refreshToken());
        UserEntity user = rotated.user();
        log.info("audit event=refresh_success");
        return ResponseEntity.ok(issuePair(user, user.getRole(), rotated.replacement()));
    }

    @PostMapping("/oauth/exchange")
    public ResponseEntity<TokenPairResponse> exchangeOAuthCode(@Valid @RequestBody OAuthExchangeRequest request) {
        OAuthLoginExchangeService service = oauthExchanges.orElseThrow(
                () -> new OAuthLoginExchangeService.ExchangeException("OAuth exchange is unavailable."));
        UserEntity user = service.consume(request.code(), request.clientId(), request.codeVerifier());
        log.info("audit event=oauth_exchange_success");
        return ResponseEntity.ok(issuePair(user, user.getRole()));
    }

    @PostMapping("/oauth/link/start")
    public ResponseEntity<OAuthLinkStartResponse> startOAuthLink(
            @Valid @RequestBody OAuthLinkStartRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BadCredentialsException("Authentication is required.");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(jwt.getSubject());
            if (userId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Authenticated subject is invalid.");
        }
        UserEntity user = users.flatMap(repository -> repository.findById(userId))
                .orElseThrow(() -> new BadCredentialsException("Authenticated user does not exist."));
        OAuthLinkIntentService service = oauthLinkIntents.orElseThrow(
                () -> new OAuthLoginExchangeService.ExchangeException("OAuth linking is unavailable."));
        String secret = service.issue(user, oauthAppClientId, request.codeChallenge());
        String url = UriComponentsBuilder.fromUriString(oauthAuthorizationBaseUrl)
                .path("/oauth2/authorization/google")
                .queryParam("client_id", oauthAppClientId)
                .queryParam("code_challenge", request.codeChallenge())
                .queryParam("link_token", secret)
                .build().encode().toUriString();
        return ResponseEntity.ok(new OAuthLinkStartResponse(url));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.ifPresent(service -> service.revoke(request.refreshToken()));
        log.info("audit event=logout");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BadCredentialsException("Authentication is required.");
        }
        String subject = jwt.getSubject();
        if (users.isPresent()) {
            try {
                Integer id = Integer.valueOf(subject);
                UserEntity user = users.get().findById(id).orElseThrow(
                        () -> new BadCredentialsException("Authenticated user does not exist."));
                return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
            } catch (NumberFormatException exception) {
                throw new BadCredentialsException("Authenticated subject is invalid.");
            }
        }
        return new UserResponse(null, subject, subject, jwt.getClaimAsString("role"));
    }

    private TokenPairResponse issuePair(UserEntity user, UserRole role) {
        String refresh = refreshTokens.map(service -> service.create(user))
                .orElseThrow(() -> new IllegalStateException("Refresh credentials are unavailable."));
        return issuePair(user, role, refresh);
    }

    private TokenPairResponse issuePair(UserEntity user, UserRole role, String refresh) {
        String access = jwtService.generateAccessToken(String.valueOf(user.getId()), role);
        return new TokenPairResponse(access, refresh, "Bearer", accessTokenMinutes * 60);
    }

    private TokenPairResponse issuePair(String subject, UserRole role) {
        String access = jwtService.generateAccessToken(subject, role);
        return new TokenPairResponse(access, "not-issued-in-memory", "Bearer", accessTokenMinutes * 60);
    }

    private static UserRole roleFromAuthentication(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElse("USER");
        return UserRole.valueOf(role);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password.length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) { super(message); }
    }
}
