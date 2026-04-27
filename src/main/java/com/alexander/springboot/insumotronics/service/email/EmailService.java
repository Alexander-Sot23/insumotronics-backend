package com.alexander.springboot.insumotronics.service.email;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendConfirmPurchase(String to, String username, String message) throws MessagingException;
    void sendCancelPurchase(String to, String username, String message) throws MessagingException;
}

