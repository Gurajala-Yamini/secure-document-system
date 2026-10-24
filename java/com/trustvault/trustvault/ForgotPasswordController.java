
package com.trustvault.trustvault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    private static final Logger logger =
            LoggerFactory.getLogger(ForgotPasswordController.class);

    private final AppUserRepository userRepository;
    private final PasswordResetRequestRepository requestRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public ForgotPasswordController(
            AppUserRepository userRepository,
            PasswordResetRequestRepository requestRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendOtp(
            @RequestParam String username,
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {

        String cleanUsername = username.trim();
        String cleanEmail = email.trim();

        var user = userRepository.findByUsername(cleanUsername)
                .filter(u -> u.getEmail() != null)
                .filter(u -> u.getEmail().equalsIgnoreCase(cleanEmail));

        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "message",
                    "If the account details match, an OTP will be sent to the registered email.");
            return "redirect:/forgot-password";
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUsername(cleanUsername);
        request.setStatus("OTP_SENT");
        request.setTokenHash(sha256(otp));
        request.setCreatedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        request.setUsed(false);

        requestRepository.save(request);

        try {
            emailService.sendOtp(cleanEmail, otp);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "OTP sent if your account details match. Check your inbox and spam folder. It expires in 5 minutes.");

            return "redirect:/reset-password";

        } catch (Exception ex) {
            request.setUsed(true);
            request.setStatus("EMAIL_FAILED");
            requestRepository.save(request);

            // Logs the real exception details in Eclipse Console.
            // Does not log the OTP or email credentials.
            logger.error("Failed to send password-reset email. Check SMTP configuration.", ex);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Email could not be sent. Check the Eclipse Console for the mail error.");

            return "redirect:/forgot-password";
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash OTP", ex);
        }
    }
}
