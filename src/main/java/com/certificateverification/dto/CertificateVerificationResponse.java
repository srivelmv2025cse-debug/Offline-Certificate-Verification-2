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
}
