package com.jobflow.controller;

import com.jobflow.api.auth.LoginRequest;
import com.jobflow.api.auth.RefreshRequest;
import com.jobflow.api.auth.RegisterRequest;
import com.jobflow.api.auth.TokenPairResponse;
import com.jobflow.api.auth.UserResponse;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.entity.UserRole;
import com.jobflow.persistence.repository.UserJpaRepository;
import com.jobflow.service.JwtService;
import com.jobflow.service.RefreshTokenService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtService jwtService;
    private final Optional<UserJpaRepository> users;
    private final Optional<RefreshTokenService> refreshTokens;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final long accessTokenMinutes;

    @Autowired
    public AuthController(JwtService jwtService,
                          @Autowired(required = false) UserJpaRepository users,
                          @Autowired(required = false) RefreshTokenService refreshTokens,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          @Value("${jobflow.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.jwtService = jwtService;
        this.users = Optional.ofNullable(users);
        this.refreshTokens = Optional.ofNullable(refreshTokens);
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
            throw new DuplicateEmailException("An account with that email already exists.");
        }
        UserEntity user = new UserEntity(request.name().trim(), email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        try {
            user = users.get().saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException("An account with that email already exists.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getName(), user.getEmail(), "USER"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (users.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        UserEntity user = users.get().findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return ResponseEntity.ok(issuePair(user, user.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService service = refreshTokens.orElseThrow(
                () -> new RefreshTokenService.RefreshTokenException("Refresh credentials are unavailable."));
        RefreshTokenService.RotatedUser rotated = service.rotate(request.refreshToken());
        UserEntity user = rotated.user();
        return ResponseEntity.ok(issuePair(user, user.getRole(), rotated.replacement()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokens.ifPresent(service -> service.revoke(request.refreshToken()));
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
