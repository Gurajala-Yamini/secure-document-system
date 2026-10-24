package com.trustvault.trustvault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Fallback default value added so the app won't crash if spring.mail.username is missing
    @Value("${spring.mail.username:no-reply@trustvault.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("TrustVault Password Reset OTP");
        message.setText(
            "Hello,\n\n" +
            "Your TrustVault password reset OTP is: " + otp + "\n\n" +
            "This OTP expires in 5 minutes. Do not share it with anyone.\n\n" +
            "If you did not request this, please ignore this email."
        );

        mailSender.send(message);
    }
}