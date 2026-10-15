package com.mulmi.backend.domain.user.service;

public interface PasswordResetEmailSender {

    void sendVerificationCode(String recipientEmail, String verificationCode);
}
