package com.certificateverification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a single block in the local blockchain.
 * Each block holds a reference to a certificate transaction.
 * Day 1: Structure only - blockchain logic implemented in Day 2+.
 */
@Entity
@Table(name = "blockchain_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Position of this block in the chain (genesis = 0) */
    @Column(name = "block_index", nullable = false, unique = true)
    private Long blockIndex;

    /** SHA-256 hash of this block */
    @Column(name = "current_hash", length = 64, nullable = false)
    private String currentHash;

    /** SHA-256 hash of the previous block (links the chain) */
    @Column(name = "previous_hash", length = 64, nullable = false)
    private String previousHash;

    /** Data stored in this block (certificate transaction JSON) */
    @Column(name = "data", columnDefinition = "TEXT", nullable = false)
    private String data;

    /** Proof-of-work nonce (for basic PoW - Day 2+) */
    @Column(name = "nonce")
    private Long nonce = 0L;

    /** Timestamp when this block was mined */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
