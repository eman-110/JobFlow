package com.jobflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jobflow")
public class JobFlowProperties {
    @NotBlank
    private String name = "JobFlow";

    @NotBlank
    private String version = "1.0.0";

    @NotBlank
    private String environment = "development";

    @Positive
    private int maxJobs = 100;

    @Positive
    private double minimumSalary = 100000;

    @Valid
    private Notifications notifications = new Notifications();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getMaxJobs() {
        return maxJobs;
    }

    public void setMaxJobs(int maxJobs) {
        this.maxJobs = maxJobs;
    }

    public double getMinimumSalary() {
        return minimumSalary;
    }

    public void setMinimumSalary(double minimumSalary) {
        this.minimumSalary = minimumSalary;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public boolean isNotificationsEnabled() {
        return notifications.isEnabled();
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        notifications.setEnabled(notificationsEnabled);
    }

    public boolean getNotificationsEnabled() {
        return notifications.isEnabled();
    }

    public static class Notifications {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
