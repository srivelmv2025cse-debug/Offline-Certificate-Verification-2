package com.certificateverification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an academic or professional certificate.
 * Day 2: Added studentName, certificateType, expiryDate, status.
 */
@Entity
@Table(name = "certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique certificate identifier (UUID) */
    @Column(name = "certificate_id", nullable = false, unique = true)
    private String certificateId;

    /** Full name of the certificate holder (student) */
    @Column(name = "student_name", nullable = false)
    private String studentName;

    /** Name of the issuing institution */
    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    /** Title/name of the course or degree */
    @Column(name = "course_name", nullable = false)
    private String courseName;

    /** Type of certificate (e.g., Degree, Diploma, Participation) */
    @Column(name = "certificate_type", nullable = false)
    private String certificateType;

    /** Date the certificate was issued */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /** Date the certificate expires (if applicable) */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Status of the certificate (e.g., ISSUED, REVOKED) */
    @Column(name = "status", nullable = false)
    private String status = "ISSUED";

    /**
     * SHA-256 hash of the certificate data stored on the blockchain.
     * Used for integrity verification.
     */
    @Column(name = "blockchain_hash", length = 64)
    private String blockchainHash;

    /**
     * Digital signature of the certificate (Base64-encoded).
     * Used for authenticity verification.
     */
    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    /**
     * QR code data (Base64-encoded PNG image).
     * Enables offline verification.
     */
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    /** Whether this certificate has been revoked */
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    /** Reason for revocation entered by institution/admin */
    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    /** Timestamp when certificate was revoked */
    @Column(name = "revocation_timestamp")
    private LocalDateTime revocationTimestamp;

    /** Timestamp of record creation */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp of last update */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Checks if the certificate is expired based on current date.
     */
    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status) || (expiryDate != null && expiryDate.isBefore(LocalDate.now()));
    }

    /**
     * Returns the effective status: REVOKED, EXPIRED, or VALID (or ISSUED).
     */
    public String getEffectiveStatus() {
        if (revoked || "REVOKED".equalsIgnoreCase(status)) {
            return "REVOKED";
        }
        if (isExpired()) {
            return "EXPIRED";
        }
        return "VALID";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
