
package com.trustvault.trustvault.service;

import com.trustvault.trustvault.AppUser;
import com.trustvault.trustvault.AppUserRepository;
import com.trustvault.trustvault.AuditLog;
import com.trustvault.trustvault.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final AppUserRepository userRepository;

    public AuditService(
            AuditLogRepository repository,
            AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public void logAction(
            String username, String action, String documentTitle) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        AuditLog log = new AuditLog(
                username,
                action,
                documentTitle,
                user.getCompany()
        );

        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return repository.findAllByOrderByActionTimeDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByCompany(Long companyId) {
        return repository
                .findByCompanyIdOrderByActionTimeDesc(companyId);
    }
}
