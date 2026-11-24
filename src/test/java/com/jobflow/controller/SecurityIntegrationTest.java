package com.jobflow.controller;

import com.jobflow.JobFlowApplication;
import com.jobflow.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {
    private static final String USERNAME = "test-user@jobflow.local";
    private static final String PASSWORD = "test-user-password";
    private static final String RECRUITER = "test-recruiter@jobflow.local";
    private static final String RECRUITER_PASSWORD = "test-recruiter-password";
    private static final String ADMIN = "test-admin@jobflow.local";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobService jobService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearJobsThroughAuthorizedApi() throws Exception {
        for (var job : jobService.getAllJobs()) {
            mockMvc.perform(delete("/api/jobs/{id}", job.getId())
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void anonymousUserCanViewJobs() throws Exception {
        int id = createJob();
        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Recruiter-created"));
    }

    @Test
    void publicCsrfEndpointReturnsTokenForStateChangingRequests() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void tokenFromCsrfEndpointAuthorizesStateChangingRequest() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Csrf-token-authorized", 100000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Csrf-token-authorized"));
    }

    @Test
    void authenticatedStateChangingRequestWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Missing CSRF", 100000)))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousUserCannotCreateJobs() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Anonymous", 100000)))
                .andExpect(status().isUnauthorized());
        assertTrue(jobService.getAllJobs().isEmpty());
    }

    @Test
    void userCannotCreateJobs() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("User", 100000)))
                .andExpect(status().isForbidden());
        assertTrue(jobService.getAllJobs().isEmpty());
    }

    @Test
    void recruiterCanCreateAndReadBackJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Recruiter-created", 100000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Recruiter-created"))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Recruiter-created"));
    }

    @Test
    void adminCanCreateJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Admin-created", 110000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Admin-created"));
    }

    @Test
    void userCannotUpdateJobs() throws Exception {
        int id = createJob();
        mockMvc.perform(put("/api/jobs/{id}", id)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Changed", 120000)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Recruiter-created"));
    }

    @Test
    void recruiterCanUpdateJobsAndReadBackChanges() throws Exception {
        int id = createJob();
        mockMvc.perform(put("/api/jobs/{id}", id)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Recruiter-updated", 130000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Recruiter-updated"));
        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(130000.0));
    }

    @Test
    void userCannotDeleteJobs() throws Exception {
        int id = createJob();
        mockMvc.perform(delete("/api/jobs/{id}", id)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jobs/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void recruiterCannotDeleteJobs() throws Exception {
        int id = createJob();
        mockMvc.perform(delete("/api/jobs/{id}", id)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jobs/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteJobsAndDeletedJobIsGone() throws Exception {
        int id = createJob();
        mockMvc.perform(delete("/api/jobs/{id}", id)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/jobs/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void missingJwtDoesNotAuthenticate() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Wrong password", 100000)))
                .andExpect(status().isUnauthorized());
        assertTrue(jobService.getAllJobs().isEmpty());
    }

    @Test
    void passwordEncoderVerifiesOnlyTheOriginalPassword() {
        String encoded = passwordEncoder.encode(PASSWORD);
        assertNotEquals(PASSWORD, encoded);
        assertTrue(passwordEncoder.matches(PASSWORD, encoded));
        assertFalse(passwordEncoder.matches("incorrect-password", encoded));
    }

    private int createJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_RECRUITER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Recruiter-created", 100000)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
    }

    private static String jobJson(String title, double salary) {
        return """
                {
                  "title": "%s",
                  "company": "Security Test Co",
                  "location": "Remote",
                  "salary": %s
                }
                """.formatted(title, salary);
    }
}
