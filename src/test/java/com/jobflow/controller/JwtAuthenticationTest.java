package com.jobflow.controller;

import com.jobflow.JobFlowApplication;
import com.jobflow.persistence.repository.RefreshTokenJpaRepository;
import com.jobflow.persistence.repository.UserJpaRepository;
import com.jobflow.service.JwtService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    @Autowired UserJpaRepository users;
    @Autowired RefreshTokenJpaRepository refreshTokens;

    @BeforeEach
    void clearAccounts() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void validRegistrationReturnsCreatedAndSafeUser() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Example User","email":"User@Example.com","password":"ExamplePassword123!"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void invalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Example","email":"not-an-email","password":"ExamplePassword123!"}
                """)).andExpect(status().isBadRequest());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Example","email":"short@example.com","password":"123"}
                """)).andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        register("duplicate@example.com");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Again","email":"DUPLICATE@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isConflict());
    }

    @Test
    void registrationIgnoresAdminPrivilegeAttempt() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Attacker","email":"attacker@example.com","password":"ExamplePassword123!","role":"ADMIN"}
                """)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginReturnsRealSignedTokenPair() throws Exception {
        register("login@example.com");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"login@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void unknownEmailAndWrongPasswordReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"unknown@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isUnauthorized());
        register("known@example.com");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"known@example.com","password":"WrongPassword123!"}
                """)).andExpect(status().isUnauthorized());
    }

    @Test
    void validSignedJwtGetsProtectedMeProfile() throws Exception {
        register("me@example.com");
        String access = loginAndGetAccess("me@example.com");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void missingAndTamperedJwtAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer abc.tampered.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredIssuerAndAudienceClaimsAreRejected() throws Exception {
        Instant now = Instant.now();
        assertClaimRejected(claims(now.minusSeconds(3600), now.minusSeconds(1800), "jobflow", "jobflow-api", "1"));
        assertClaimRejected(claims(now, now.plusSeconds(900), "wrong-issuer", "jobflow-api", "1"));
        assertClaimRejected(claims(now, now.plusSeconds(900), "jobflow", "wrong-audience", "1"));
    }

    @Test
    void missingRequiredRoleOrTypeClaimIsRejected() throws Exception {
        JwtClaimsSet.Builder b = JwtClaimsSet.builder()
                .issuer("jobflow").audience(List.of("jobflow-api")).subject("1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900))
                .claim("token_type", "access");
        String missingRole = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).keyId("jobflow-hs256").build(), b.build())).getTokenValue();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + missingRole))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotationReuseAndLogoutAreEnforced() throws Exception {
        register("rotate@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"rotate@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk()).andReturn();
        String body = login.getResponse().getContentAsString();
        String refresh = extract(body, "refreshToken");

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"does-not-exist"}
                """)).andExpect(status().isNoContent());
    }

    @Test
    void passwordIsStoredAsBcryptHashOnly() throws Exception {
        register("hash@example.com");
        var user = users.findByEmailIgnoreCase("hash@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("ExamplePassword123!", user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(user.getPasswordHash().startsWith("$2"));
    }

    @Test
    void missingJwtCannotCreateJob() throws Exception {
        mockMvc.perform(post("/api/jobs").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"No auth","company":"Test","location":"Remote","salary":100000}
                """)).andExpect(status().isUnauthorized());
    }

    @Test
    void userJwtCannotCreateJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + jwtService.generateAccessToken("101", com.jobflow.persistence.entity.UserRole.USER))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"User job","company":"Test","location":"Remote","salary":100000}
                """)).andExpect(status().isForbidden());
    }

    @Test
    void recruiterJwtCanCreateJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + jwtService.generateAccessToken("102", com.jobflow.persistence.entity.UserRole.RECRUITER))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Recruiter job","company":"Test","location":"Remote","salary":100000}
                """)).andExpect(status().isCreated());
    }

    @Test
    void adminJwtCanDeleteJob() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + jwtService.generateAccessToken("103", com.jobflow.persistence.entity.UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Delete me","company":"Test","location":"Remote","salary":100000}
                """)).andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");
        int id = Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/jobs/{id}", id)
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken("103", com.jobflow.persistence.entity.UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        register("expired-refresh@example.com");
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"not-a-stored-token"}
                """)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutThenRefreshIsRejected() throws Exception {
        register("logout@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"logout@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk()).andReturn();
        String refresh = extract(login.getResponse().getContentAsString(), "refreshToken");
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectSigningKeyIsRejected() throws Exception {
        byte[] wrongKey = "This-key-is-not-the-application-signing-key-32b".getBytes();
        var key = new OctetSequenceKey.Builder(wrongKey)
                .keyID("wrong-key").algorithm(com.nimbusds.jose.JWSAlgorithm.HS256).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        JwtClaimsSet claims = claims(Instant.now(), Instant.now().plusSeconds(900),
                "jobflow", "jobflow-api", "1").build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).keyId("wrong-key").build(), claims)).getTokenValue();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRouteIsDenied() throws Exception {
        mockMvc.perform(get("/api/not-a-jobflow-route")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenDatabaseStoresHashOnly() throws Exception {
        register("hash-refresh@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"hash-refresh@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk()).andReturn();
        String refresh = extract(login.getResponse().getContentAsString(), "refreshToken");
        var stored = refreshTokens.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertEquals(64, stored.getTokenHash().length());
        org.junit.jupiter.api.Assertions.assertNotEquals(refresh, stored.getTokenHash());
    }

    @Test
    void rotatedRefreshTokenReuseIsRejected() throws Exception {
        register("reuse@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"reuse@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk()).andReturn();
        String refresh = extract(login.getResponse().getContentAsString(), "refreshToken");
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshRequestsAllowAtMostOneSuccess() throws Exception {
        register("concurrent@example.com");
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"concurrent@example.com","password":"ExamplePassword123!"}
                """)).andExpect(status().isOk()).andReturn();
        String refresh = extract(login.getResponse().getContentAsString(), "refreshToken");
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> refreshConcurrently(refresh, ready, start));
            var second = executor.submit(() -> refreshConcurrently(refresh, ready, start));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertEquals(1, successes);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void refreshRejectedCredentialReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"invalid-credential"}
                """)).andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Test User","email":"%s","password":"ExamplePassword123!"}
                """.formatted(email))).andExpect(status().isCreated());
    }

    private int refreshConcurrently(String refresh, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent refresh test did not start.");
        }
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("""
                {"refreshToken":"%s"}
                """.formatted(refresh))).andReturn().getResponse().getStatus() == 200 ? 1 : 0;
    }

    private String loginAndGetAccess(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"ExamplePassword123!"}
                """.formatted(email))).andExpect(status().isOk()).andReturn();
        return extract(result.getResponse().getContentAsString(), "accessToken");
    }

    private void assertClaimRejected(JwtClaimsSet.Builder builder) throws Exception {
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).keyId("jobflow-hs256").build(), builder.build())).getTokenValue();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private JwtClaimsSet.Builder claims(Instant issued, Instant expires, String issuer, String audience, String subject) {
        return JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(subject)
                .issuedAt(issued).expiresAt(expires).claim("token_type", "access").claim("role", "USER");
    }

    private static String extract(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Missing field " + field);
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
