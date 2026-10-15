package com.mulmi.backend.domain.user.service;

import com.mulmi.backend.domain.user.exception.UserException;
import com.mulmi.backend.domain.user.exception.code.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Override
    public void sendVerificationCode(String recipientEmail, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!senderEmail.isBlank()) {
            message.setFrom(senderEmail);
        }
        message.setTo(recipientEmail);
        message.setSubject("[멀티미디어실] 비밀번호 재설정 인증번호");
        message.setText("""
                비밀번호 재설정 인증번호는 %s입니다.

                인증번호는 5분 동안 유효합니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(verificationCode));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new UserException(UserErrorCode.PASSWORD_RESET_MAIL_FAILED);
        }
    }
}
