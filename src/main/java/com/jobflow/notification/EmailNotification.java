package com.jobflow.notification;

import org.springframework.stereotype.Component;

@Component("emailNotification")
public class EmailNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("EMAIL: " + message);
    }
}
