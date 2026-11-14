package com.jobflow.controller;

import com.jobflow.JobFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void shouldCreateJobAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content(jobJson("Java Developer", "Google", "London", 120000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.company").value("Google"))
                .andExpect(jsonPath("$.location").value("London"))
                .andExpect(jsonPath("$.salary").value(120000.0))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void shouldReturnExistingJobAndNotFoundForMissingJob() throws Exception {
        mockMvc.perform(get("/api/jobs/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.title").value("Java Developer"));

        mockMvc.perform(get("/api/jobs/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateExistingJobAndReturnNotFoundForMissingJob() throws Exception {
        int id = createJob("Original title", "Original company", "Original location", 90000);

        mockMvc.perform(put("/api/jobs/{id}", id)
                        .contentType(APPLICATION_JSON)
                        .content(jobJson("Updated title", "Updated company", "Updated location", 110000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.company").value("Updated company"))
                .andExpect(jsonPath("$.location").value("Updated location"))
                .andExpect(jsonPath("$.salary").value(110000.0))
                .andExpect(jsonPath("$.status").value("SAVED"));

        mockMvc.perform(put("/api/jobs/999999")
                        .contentType(APPLICATION_JSON)
                        .content(jobJson("Updated title", "Updated company", "Updated location", 110000)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPatchSalaryWithoutChangingOtherJobDetails() throws Exception {
        int id = createJob("Salary patch title", "Salary patch company", "Salary patch location", 90000);
        mockMvc.perform(patch("/api/jobs/{id}/status", id)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/jobs/{id}/salary", id)
                        .contentType(APPLICATION_JSON)
                        .content("{\"salary\":125000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Salary patch title"))
                .andExpect(jsonPath("$.company").value("Salary patch company"))
                .andExpect(jsonPath("$.location").value("Salary patch location"))
                .andExpect(jsonPath("$.salary").value(125000.0))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void shouldRejectInvalidStatusTransitionsAndPreserveJobDetails() throws Exception {
        int id = createJob("Status title", "Status company", "Status location", 100000);

        mockMvc.perform(patch("/api/jobs/{id}/status", id)
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Status title"))
                .andExpect(jsonPath("$.company").value("Status company"))
                .andExpect(jsonPath("$.location").value("Status location"))
                .andExpect(jsonPath("$.salary").value(100000.0))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void shouldDeleteJobAndReturnNotFoundWhenRepeated() throws Exception {
        int id = createJob("Delete title", "Delete company", "Delete location", 80000);

        mockMvc.perform(delete("/api/jobs/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/jobs/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAllJobsAsJsonArray() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
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

    private int createJob(String title, String company, String location, double salary) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content(jobJson(title, company, location, salary)))
                .andExpect(status().isCreated())
                .andReturn();
        return Integer.parseInt(URI.create(result.getResponse().getHeader("Location"))
                .getPath()
                .substring("/api/jobs/".length()));
    }

    private static String jobJson(String title, String company, String location, double salary) {
        return """
                {
                  "title": "%s",
                  "company": "%s",
                  "location": "%s",
                  "salary": %s
                }
                """.formatted(title, company, location, salary);
    }
}
