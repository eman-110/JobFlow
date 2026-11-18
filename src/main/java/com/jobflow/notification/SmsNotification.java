package com.jobflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("smsNotification")
public class SmsNotification implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotification.class);

    @Override
    public void sendNotification(String message) {
        logger.info("SMS: {}", message);
    }
}
