package com.jobflow.controller;

import com.jobflow.JobFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = JobFlowApplication.class)
@AutoConfigureMockMvc
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
    void shouldReturnJobByIdOrNotFound() throws Exception {
        mockMvc.perform(get("/api/jobs/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.title").value("Java Developer"));

        mockMvc.perform(get("/api/jobs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldBindCompanyFromQueryParameter() throws Exception {
        mockMvc.perform(get("/api/jobs/search").param("company", "tEcH sOlUtIoNs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company").value("Tech Solutions"));
    }

    @Test
    void shouldFilterJobsByMinimumSalary() throws Exception {
        mockMvc.perform(get("/api/jobs/high-paying").param("minimumSalary", "150000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].salary").value(150000.0));
    }
}
