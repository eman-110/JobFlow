package com.jobflow.controller;

import com.jobflow.api.CsrfTokenResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfTokenController {
    @GetMapping("/api/csrf")
    public CsrfTokenResponse getCsrfToken(CsrfToken csrfToken) {
        // Bearer authentication is stateless and is not vulnerable to browser-cookie
        // CSRF. This endpoint remains as a compatibility notice for older clients.
        return new CsrfTokenResponse("X-CSRF-TOKEN", "disabled-for-bearer-api");
    }
}
