package com.certificateverification.offline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plain POJO representing a blockchain block in the offline sync package.
 * Not a JPA entity — used exclusively for JSON serialization/deserialization
 * during offline blockchain synchronization.
 *
 * <p>Day 8: Offline Verification and Blockchain Synchronization.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineBlock {

    /** Block sequence number in the chain (0 for genesis) */
    private int index;

    /** Timestamp of block creation in milliseconds */
    private long timestamp;

    /** Certificate ID associated with this block */
    private String certificateId;

    /** SHA-256 hash of the canonical certificate data */
    private String certificateHash;

    /** SHA-256 hash of the preceding block in the chain */
    private String previousHash;

    /** SHA-256 hash of this block */
    private String hash;
}
