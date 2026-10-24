
package com.trustvault.trustvault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResetPasswordController {

    private final AppUserRepository userRepository;
    private final PasswordResetRequestRepository requestRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordController(
            AppUserRepository userRepository,
            PasswordResetRequestRepository requestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String updatePassword(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        String cleanUsername = username.trim();
        String cleanEmail = email.trim();

        if (newPassword.length() < 8) {
            model.addAttribute("message",
                    "Password must contain at least 8 characters.");
            return "reset-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("message", "Passwords do not match.");
            return "reset-password";
        }

        var user = userRepository.findByUsername(cleanUsername)
                .filter(u -> u.getEmail() != null)
                .filter(u -> u.getEmail().equalsIgnoreCase(cleanEmail));

        if (user.isEmpty()) {
            model.addAttribute("message", "Invalid details or OTP.");
            return "reset-password";
        }

        var request = requestRepository
                .findFirstByUsernameAndStatusOrderByCreatedAtDesc(
                        cleanUsername, "OTP_SENT");

        if (request.isEmpty()
                || request.get().getUsed()
                || request.get().getExpiresAt() == null
                || !request.get().getExpiresAt().isAfter(LocalDateTime.now())
                || request.get().getTokenHash() == null
                || !MessageDigest.isEqual(
                        sha256(otp.trim()).getBytes(StandardCharsets.UTF_8),
                        request.get().getTokenHash()
                                .getBytes(StandardCharsets.UTF_8))) {

            model.addAttribute("message",
                    "Invalid or expired OTP. Please request a new one.");
            return "reset-password";
        }

        AppUser appUser = user.get();
        appUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(appUser);

        PasswordResetRequest resetRequest = request.get();
        resetRequest.setUsed(true);
        resetRequest.setStatus("COMPLETED");
        requestRepository.save(resetRequest);

        redirectAttributes.addFlashAttribute("message",
                "Password reset successful. Please log in.");

        return "redirect:/login";
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
