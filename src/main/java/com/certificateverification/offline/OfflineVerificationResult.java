package com.certificateverification.offline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Structured result of an offline certificate verification attempt.
 *
 * <p>Day 8: Mirrors the online {@code CertificateVerificationResponse}
 * but is computed entirely from the local sync package — no Internet calls.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineVerificationResult {

    /**
     * Primary verdict:
     * GENUINE, TAMPERED, NOT_FOUND, REVOKED, EXPIRED
     */
    private String result;

    private String certificateId;
    private String studentName;
    private String courseName;
    private String institutionName;

    /** Whether the recalculated hash matched the blockchain snapshot */
    private boolean blockchainMatch;

    /** Whether the digital signature was verified against the local public key */
    private boolean signatureValid;

    /** Human-readable signature status */
    private String signatureStatus;

    /** Certificate lifecycle status */
    private String certificateStatus;

    /** Human-readable explanation */
    private String message;

    /** When the offline verification was performed */
    @Builder.Default
    private LocalDateTime verifiedAt = LocalDateTime.now();
}
