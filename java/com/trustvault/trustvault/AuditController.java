
package com.trustvault.trustvault;

import com.trustvault.trustvault.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditController {

    private final AuditService auditService;
    private final AppUserRepository userRepository;

    public AuditController(
            AuditService auditService,
            AppUserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping("/audit-logs")
    public String showAuditLogs(
            Model model,
            Authentication authentication) {

        AppUser user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (user.getCompany() == null) {
            throw new IllegalStateException(
                    "User is not assigned to a company.");
        }

        model.addAttribute(
                "logs",
                auditService.getLogsByCompany(
                        user.getCompany().getId()));

        return "audit-logs";
    }
}
