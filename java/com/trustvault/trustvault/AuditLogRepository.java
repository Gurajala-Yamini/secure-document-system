
package com.trustvault.trustvault;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByActionTimeDesc();

    List<AuditLog> findByCompanyIdOrderByActionTimeDesc(Long companyId);
}
