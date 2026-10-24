
package com.trustvault.trustvault;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPasswordResetController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminPasswordResetController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Temporary diagnostic test: method-level authorization removed
    @GetMapping("/admin/password-reset")
    public String showResetPage() {
        return "admin-password-reset";
    }

    @PostMapping("/admin/password-reset")
    public String resetPassword(
            @RequestParam String username,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        username = username.trim();

        if (username.isBlank() || newPassword.isBlank()
                || confirmPassword.isBlank()) {
            model.addAttribute("error",
                    "Please fill in all fields.");
            return "admin-password-reset";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error",
                    "Passwords do not match.");
            return "admin-password-reset";
        }

        if (newPassword.length() < 8) {
            model.addAttribute("error",
                    "Password must contain at least 8 characters.");
            return "admin-password-reset";
        }

        AppUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            model.addAttribute("error", "User not found.");
            return "admin-password-reset";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "success", "Password reset successfully.");

        return "redirect:/admin/password-reset";
    }
}
