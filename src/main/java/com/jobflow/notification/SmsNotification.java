package com.jobflow.notification;

import org.springframework.stereotype.Component;

@Component("smsNotification")
public class SmsNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("SMS: " + message);
    }
}
