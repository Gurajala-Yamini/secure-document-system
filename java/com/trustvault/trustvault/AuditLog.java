
package com.trustvault.trustvault;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String documentTitle;

    @Column(nullable = false)
    private LocalDateTime actionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    public AuditLog() {
    }

    public AuditLog(String username, String action,
                    String documentTitle, Company company) {
        this.username = username;
        this.action = action;
        this.documentTitle = documentTitle;
        this.company = company;
        this.actionTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
