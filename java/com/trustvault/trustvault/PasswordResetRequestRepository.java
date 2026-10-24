
package com.trustvault.trustvault;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRequestRepository
        extends JpaRepository<PasswordResetRequest, Long> {

    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(
            String status);

    Optional<PasswordResetRequest> findByTokenHash(String tokenHash);

    Optional<PasswordResetRequest>
        findFirstByUsernameAndStatusOrderByCreatedAtDesc(
            String username, String status);
}
