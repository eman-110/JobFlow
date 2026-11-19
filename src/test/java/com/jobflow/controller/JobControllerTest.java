package com.jobflow.controller;

import com.jobflow.exception.JobCapacityExceededException;
import com.jobflow.exception.JobExceptionHandler;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.model.Job;
import com.jobflow.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(JobExceptionHandler.class)
class JobControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @Test
    void shouldReturnJobAndNotFoundResponse() throws Exception {
        Job job = new Job(70, "Developer", "Example Co", "London", 100000);
        when(jobService.findJobById(70)).thenReturn(Optional.of(job));
        when(jobService.findJobById(404)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Developer"));
        mockMvc.perform(get("/api/jobs/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateUpdateAndDeleteJobs() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Developer","company":"Example Co","location":"London","salary":100000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Developer"));

        Job updated = new Job(70, "Senior Developer", "Example Co", "London", 120000);
        when(jobService.updateJob(org.mockito.ArgumentMatchers.eq(70), any(Job.class)))
                .thenReturn(updated);
        mockMvc.perform(put("/api/jobs/70")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Senior Developer","company":"Example Co","location":"London","salary":120000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salary").value(120000.0));

        mockMvc.perform(delete("/api/jobs/70"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectInvalidJobRequests() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","company":"Example Co","location":"London","salary":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMapMissingJobsAndCapacityFailuresToHttpErrors() throws Exception {
        when(jobService.updateJob(org.mockito.ArgumentMatchers.eq(404), any(Job.class)))
                .thenThrow(new JobNotFoundException("Job not found: 404"));
        doThrow(new JobCapacityExceededException("Maximum job capacity has been reached."))
                .when(jobService).addJob(any(Job.class));

        mockMvc.perform(put("/api/jobs/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Developer","company":"Example Co","location":"London","salary":100000}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Developer","company":"Example Co","location":"London","salary":100000}
                                """))
                .andExpect(status().isConflict());
    }
}
