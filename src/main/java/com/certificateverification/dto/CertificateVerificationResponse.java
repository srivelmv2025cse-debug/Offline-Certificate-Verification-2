package com.certificateverification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Structured response for certificate verification attempts.
 * Returns the exact match status, calculated and blockchain hashes,
 * and clear verdict.
 * Day 6: Added digital signature verification fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVerificationResponse {

    /**
     * Primary verification verdict:
     * - "Certificate Not Found / Potentially Fake"
     * - "GENUINE CERTIFICATE"
     * - "TAMPERED CERTIFICATE"
     * - "REVOKED CERTIFICATE"
     */
    private String result;

    /** Certificate ID verified */
    private String certificateId;

    /** Student name */
    private String student;

    @JsonProperty("studentName")
    public String getStudentName() {
        return student;
    }

    /** Institution name */
    private String institution;

    @JsonProperty("institutionName")
    public String getInstitutionName() {
        return institution;
    }

    /** Course name */
    private String course;

    @JsonProperty("courseName")
    public String getCourseName() {
        return course;
    }

    /** Issue date */
    private String issueDate;

    /** SHA-256 hash retrieved from the blockchain block */
    private String blockchainHash;

    /** SHA-256 hash recalculated from the provided verification data */
    private String calculatedHash;

    /** Whether the calculated hash matches the blockchain hash */
    private boolean blockchainMatch;

    /** Status of certificate: ISSUED, REVOKED, NOT_FOUND, TAMPERED */
    private String certificateStatus;

    /** Boolean flag indicating if certificate is genuine and valid */
    private boolean verified;

    /** Human-readable explanation of verification outcome */
    private String message;

    /** Timestamp of the verification check */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Day 6 - Digital Signature Fields
    // -------------------------------------------------------------------------

    /**
     * Whether the RSA digital signature is cryptographically valid.
     * true means the certificate was signed by the institution's private key.
     */
    private boolean signatureValid;

    /**
     * Human-readable digital signature status:
     * "VALID", "INVALID", "NOT SIGNED", or "SIGNATURE MISSING"
     */
    private String signatureStatus;

    /**
     * Name of the institution whose private key signed this certificate.
     */
    private String signingInstitution;

    /**
     * Short SHA-256 fingerprint of the institution's public key
     * (first 16 bytes, colon-delimited hex). Safe to display in UI.
     *
     * Example: A3:F1:2C:88:7D:01:FF:AB:CC:12:34:56:78:9A:BC:DE
     */
    private String publicKeyFingerprint;
}
