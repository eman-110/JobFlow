package com.jobflow.notification;

public class WhatsAppNotification implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("WHATSAPP: " + message);
    }
}
