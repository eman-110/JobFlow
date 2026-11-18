package com.jobflow.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhatsAppNotification implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppNotification.class);

    @Override
    public void sendNotification(String message) {
        logger.info("WHATSAPP: {}", message);
    }
}
