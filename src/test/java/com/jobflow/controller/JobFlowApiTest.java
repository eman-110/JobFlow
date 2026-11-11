package com.jobflow.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HelloController.class, JobController.class})
class JobFlowApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnGreeting() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from JobFlow!"));
    }

    @Test
    void shouldReturnSampleJobsAsJson() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].title").value("Java Developer"))
                .andExpect(jsonPath("$[0].company").value("Tech Solutions"))
                .andExpect(jsonPath("$[0].salary").value(150000.0));
    }

    @Test
    void shouldBindJobIdFromPath() throws Exception {
        mockMvc.perform(get("/api/jobs/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Java Developer"));
    }

    @Test
    void shouldBindCompanyFromQueryParameter() throws Exception {
        mockMvc.perform(get("/api/jobs/search").param("company", "Google"))
                .andExpect(status().isOk())
                .andExpect(content().string("Searching jobs at: Google"));
    }
}
