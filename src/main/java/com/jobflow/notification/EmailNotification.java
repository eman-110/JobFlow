package com.jobflow.notification;

public class EmailNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("EMAIL: " + message);
    }
}
