package com.certificateverification.qr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Structured response for QR certificate verification.
 * Returns simple, high-level verdicts:
 * - "Genuine"
 * - "Tampered"
 * - "Not Found"
 * - "Revoked"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRVerificationResponse {

    /**
     * Simple verification result:
     * - "Genuine"
     * - "Tampered"
     * - "Not Found"
     * - "Revoked"
     * - "Invalid QR Code" (for malformed data)
     */
    private String result;

    /** Status code enum-like string: GENUINE, TAMPERED, NOT_FOUND, REVOKED, INVALID_QR */
    private String status;

    /** Certificate ID */
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

    /** SHA-256 hash from blockchain block */
    private String blockchainHash;

    /** Verification reference extracted from QR code */
    private String qrReference;

    /** Whether QR reference matches blockchain ledger */
    private boolean blockchainMatch;

    /** Whether the certificate is genuine and valid */
    private boolean verified;

    /** Explanatory message */
    private String message;

    /** Timestamp of verification */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
