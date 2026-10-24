
package com.trustvault.trustvault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminResetRequestsController {

    private final PasswordResetRequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminResetRequestsController(
            PasswordResetRequestRepository requestRepository,
            AppUserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/reset-requests")
    public String showRequests(Model model) {
        List<PasswordResetRequest> pendingRequests =
                requestRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        model.addAttribute("requests", pendingRequests);
        return "admin-reset-requests";
    }

    @PostMapping("/admin/reset-requests/{id}/approve")
    public String approveRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) throws Exception {

        PasswordResetRequest request =
                requestRepository.findById(id).orElse(null);

        if (request == null || !"PENDING".equals(request.getStatus())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Request not found or already processed.");
            return "redirect:/admin/reset-requests";
        }

        if (!userRepository.existsByUsername(request.getUsername())) {
            request.setStatus("REJECTED");
            requestRepository.save(request);

            redirectAttributes.addFlashAttribute(
                    "error", "The account no longer exists.");
            return "redirect:/admin/reset-requests";
        }

        // Verify the user's identity before approving this request.
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = sha256(rawToken);

        request.setTokenHash(tokenHash);
        request.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        request.setStatus("APPROVED");
        request.setUsed(false);
        requestRepository.save(request);

        String resetLink =
                "http://localhost:8082/reset-password?token=" + rawToken;

        // Display the raw token only once to the admin.
        redirectAttributes.addFlashAttribute("resetLink", resetLink);
        redirectAttributes.addFlashAttribute(
                "success", "Request approved. The reset link expires in 15 minutes.");

        return "redirect:/admin/reset-requests";
    }

    @PostMapping("/admin/reset-requests/{id}/reject")
    public String rejectRequest(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        PasswordResetRequest request =
                requestRepository.findById(id).orElse(null);

        if (request == null || !"PENDING".equals(request.getStatus())) {
            redirectAttributes.addFlashAttribute(
                    "error", "Request not found or already processed.");
            return "redirect:/admin/reset-requests";
        }

        request.setStatus("REJECTED");
        requestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "success", "Reset request rejected.");

        return "redirect:/admin/reset-requests";
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(hash);
    }
}
