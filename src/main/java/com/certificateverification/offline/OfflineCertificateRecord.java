package com.certificateverification.offline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plain POJO holding certificate metadata needed for offline verification.
 * Not a JPA entity — serialized into the offline sync package JSON.
 *
 * <p>Day 8: Contains all fields required to verify a certificate offline:
 * identity fields, blockchain hash, digital signature, revocation status.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineCertificateRecord {

    private String certificateId;
    private String studentName;
    private String courseName;
    private String institutionName;
    private String certificateType;
    private String issueDate;
    private String expiryDate;
    private String status;
    private String blockchainHash;
    private String digitalSignature;
    private boolean revoked;
    private String revocationReason;
    private String revocationTimestamp;
}
