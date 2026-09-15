package com.certificateverification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry.
 * Tracks all actions performed on certificates (issue, verify, revoke).
 * Day 1: Structure only - full audit logic in Day 2+.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The certificate ID related to this log entry */
    @Column(name = "certificate_id")
    private String certificateId;

    /**
     * Action performed: ISSUED, VERIFIED, REVOKED, FAILED_VERIFICATION
     */
    @Column(name = "action", nullable = false)
    private String action;

    /** IP address of the requester */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Additional details about the action */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** Timestamp of the action */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
