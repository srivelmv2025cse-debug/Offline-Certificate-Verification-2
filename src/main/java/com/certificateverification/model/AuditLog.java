package com.certificateverification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry.
 * Tracks all actions performed on certificates (issue, verify, revoke, QR, blockchain).
 * Day 7: Full Audit Trail model with id, certificateId, action, timestamp, result, details.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The certificate ID related to this log entry */
    @Column(name = "certificate_id")
    private String certificateId;

    /**
     * Action performed: CERTIFICATE_ISSUANCE, VERIFIED, FAILED_VERIFICATION, REVOCATION, QR_VERIFICATION, BLOCKCHAIN_VALIDATION
     */
    @Column(name = "action", nullable = false)
    private String action;

    /**
     * Result of the action: SUCCESS, FAILED, VALID, TAMPERED, REVOKED, etc.
     */
    @Column(name = "result")
    private String result;

    /** IP address of the requester */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Additional details about the action */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** Timestamp of the action */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AuditLog(String certificateId, String action, String result, String details, String ipAddress) {
        this.certificateId = certificateId;
        this.action = action;
        this.result = result;
        this.details = details;
        this.ipAddress = ipAddress;
        this.timestamp = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
