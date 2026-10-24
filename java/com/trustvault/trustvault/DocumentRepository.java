
package com.trustvault.trustvault;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUploadedBy(String uploadedBy);

    List<Document> findByCompanyId(Long companyId);

    List<Document> findByCompanyIdAndUploadedBy(
            Long companyId, String uploadedBy);
}
