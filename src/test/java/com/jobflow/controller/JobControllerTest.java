package com.jobflow.controller;

import com.jobflow.exception.ApiExceptionHandler;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import com.jobflow.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(ApiExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class JobControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @Test
    void shouldCreateAJobWithLocationHeader() throws Exception {
        Job job = new Job(70, "Developer", "Example Co", "London", 100000);
        when(jobService.createJob("Developer", "Example Co", "London", 100000))
                .thenReturn(job);

        mockMvc.perform(post("/api/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Developer", "Example Co", "London", 100000)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/jobs/70"))
                .andExpect(jsonPath("$.id").value(70))
                .andExpect(jsonPath("$.title").value("Developer"));
    }

    @Test
    void shouldReturnStructuredValidationErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","company":"Example Co","location":"London","salary":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.salary").exists());
    }

    @Test
    void shouldMapMissingJobsToStructuredNotFoundResponse() throws Exception {
        when(jobService.getJobById(404))
                .thenThrow(new JobNotFoundException("Job with ID 404 was not found."));

        mockMvc.perform(get("/api/jobs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job with ID 404 was not found."));
    }

    @Test
    void shouldMapCapacityAndStatusConflictsToHttp409() throws Exception {
        when(jobService.createJob("Overflow", "Example Co", "London", 100000))
                .thenThrow(new IllegalStateException("Maximum job capacity of 4 reached."));
        when(jobService.updateStatus(70, JobStatus.INTERVIEW))
                .thenThrow(new IllegalStateException(
                        "Cannot change job status from SAVED to INTERVIEW."
                ));

        mockMvc.perform(post("/api/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson("Overflow", "Example Co", "London", 100000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(patch("/api/jobs/70/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot change job status from SAVED to INTERVIEW."));
    }

    @Test
    void shouldKeepPatchEndpointsAvailable() throws Exception {
        Job updatedJob = new Job(70, "Developer", "Example Co", "London", 125000);
        updatedJob.updateStatus(JobStatus.APPLIED);
        when(jobService.updateSalary(70, 125000)).thenReturn(updatedJob);
        when(jobService.updateStatus(eq(70), eq(JobStatus.APPLIED))).thenReturn(updatedJob);

        mockMvc.perform(patch("/api/jobs/70/salary")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salary\":125000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(125000.0));
        mockMvc.perform(patch("/api/jobs/70/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
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
