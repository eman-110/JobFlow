package com.jobflow.notification;

public class SmsNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("SMS: " + message);
    }
}
