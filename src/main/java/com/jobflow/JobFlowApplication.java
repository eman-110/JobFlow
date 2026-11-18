package com.jobflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JobFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobFlowApplication.class, args);
    }
}
