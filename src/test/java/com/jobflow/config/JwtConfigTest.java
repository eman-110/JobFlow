package com.jobflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {
    private final JwtConfig config = new JwtConfig();
    private final MockEnvironment mysqlEnvironment = new MockEnvironment();

    JwtConfigTest() {
        mysqlEnvironment.setActiveProfiles("mysql");
    }

    @Test
    void requiresSecretOutsideTestProfile() {
        assertThrows(IllegalStateException.class, () -> config.resolveSecret("", mysqlEnvironment));
    }

    @Test
    void requiresBase64Secret() {
        assertThrows(IllegalArgumentException.class,
                () -> config.resolveSecret("not-base64!", mysqlEnvironment));
    }

    @Test
    void requiresAtLeastThirtyTwoDecodedBytes() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());
        assertThrows(IllegalArgumentException.class,
                () -> config.resolveSecret(shortSecret, mysqlEnvironment));
    }

    @Test
    void acceptsAtLeastThirtyTwoDecodedBytes() {
        String secret = "4Bk/UD43/8ySHPhA+W3IHDYXk/Ce0oEg2Vm2rTjAlBU=";
        assertEquals(32, config.resolveSecret(secret, mysqlEnvironment).length);
    }
}
