package com.jobflow.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobFlowConfigurationTest {

    @Test
    void shouldLoadApplicationProperties() {
        AppConfig config = new AppConfig();

        assertEquals("JobFlow", config.getName());
        assertEquals("1.0", config.getVersion());
        assertEquals(100000.0, config.getMinimumSalary());
    }
}
