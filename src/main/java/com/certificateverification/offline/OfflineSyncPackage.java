package com.certificateverification.offline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Wrapper POJO for the complete offline sync package.
 * Serialized to / deserialized from {@code offline_sync/blockchain_snapshot.json}.
 *
 * <p>Day 8: Contains the entire trusted snapshot required for offline verification:</p>
 * <ul>
 *   <li>Blockchain blocks (ordered by index)</li>
 *   <li>Certificate records (with revocation info)</li>
 *   <li>Institution public keys (Base64-encoded)</li>
 *   <li>Summary statistics</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSyncPackage {

    /** ISO-8601 timestamp when this package was generated */
    private String generatedAt;

    /** Package format version */
    @Builder.Default
    private String version = "1.0";

    /** Complete list of blockchain blocks in chain order */
    private List<OfflineBlock> blockchainBlocks;

    /** Complete list of certificate records */
    private List<OfflineCertificateRecord> certificates;

    /**
     * Map of institution name → Base64-encoded RSA public key.
     * Used for offline digital signature verification.
     */
    private Map<String, String> publicKeys;

    /** Total number of blockchain blocks in the snapshot */
    private int totalBlocks;

    /** Total number of certificate records in the snapshot */
    private int totalCertificates;

    /** Total number of revoked certificates in the snapshot */
    private int totalRevokedCertificates;
}
