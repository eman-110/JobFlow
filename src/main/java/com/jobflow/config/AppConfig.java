package com.jobflow.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties was not found.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load application.properties.", e);
        }
    }

    public String getName() {
        return properties.getProperty("app.name");
    }

    public String getVersion() {
        return properties.getProperty("app.version");
    }

    public double getMinimumSalary() {
        return Double.parseDouble(properties.getProperty("app.minimum.salary"));
    }
}
