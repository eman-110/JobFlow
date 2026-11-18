package com.jobflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("emailNotification")
public class EmailNotification implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotification.class);

    @Override
    public void sendNotification(String message) {
        logger.info("EMAIL: {}", message);
    }
}
