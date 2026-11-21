package com.jobflow.persistence.repository;

import com.jobflow.model.Job;
import com.jobflow.persistence.entity.CompanyEntity;
import com.jobflow.persistence.entity.JobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaJobStoreTest {
    @Mock
    private JobJpaRepository jobs;

    @Mock
    private CompanyJpaRepository companies;

    private JpaJobStore store;

    @BeforeEach
    void setUp() {
        store = new JpaJobStore(jobs, companies);
    }

    @Test
    void rejectsUnknownCompanyInsteadOfCreatingOne() {
        when(companies.findByName("Unknown Co")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> store.add(new Job("Developer", "Unknown Co", "Remote", 100000)));

        assertEquals("Company 'Unknown Co' does not exist.", exception.getMessage());
        verify(companies, never()).saveAndFlush(any(CompanyEntity.class));
        verify(jobs, never()).saveAndFlush(any(JobEntity.class));
    }

    @Test
    void assignsTheDatabaseGeneratedIdWhenAddingAJob() {
        CompanyEntity company = new CompanyEntity(12, "Known Co", null, "US");
        when(companies.findByName("Known Co")).thenReturn(Optional.of(company));
        when(jobs.saveAndFlush(any(JobEntity.class))).thenAnswer(invocation -> {
            JobEntity input = invocation.getArgument(0);
            return new JobEntity(
                    73,
                    input.getCompanyId(),
                    input.getTitle(),
                    input.getLocation(),
                    input.getSalary()
            );
        });
        Job job = new Job("Developer", "Known Co", "Remote", 100000);

        store.add(job);

        ArgumentCaptor<JobEntity> entity = ArgumentCaptor.forClass(JobEntity.class);
        verify(jobs).saveAndFlush(entity.capture());
        assertNull(entity.getValue().getId());
        assertEquals(12, entity.getValue().getCompanyId());
        assertEquals(73, job.getId());
    }

    @Test
    void resolvesCompanyNamesInOneBatchWhenReadingJobs() {
        JobEntity first = new JobEntity(1, 12, "Backend Developer", "Remote", BigDecimal.ONE);
        JobEntity second = new JobEntity(2, 12, "Frontend Developer", "Remote", BigDecimal.TEN);
        CompanyEntity company = new CompanyEntity(12, "Known Co", null, "US");
        when(jobs.findAll()).thenReturn(List.of(first, second));
        when(companies.findAllById(any(Iterable.class))).thenReturn(List.of(company));

        List<Job> result = store.findAll();

        assertEquals(List.of("Known Co", "Known Co"),
                result.stream().map(Job::getCompany).toList());
        verify(companies, times(1)).findAllById(any(Iterable.class));
        verify(companies, never()).findById(anyInt());
    }
}
