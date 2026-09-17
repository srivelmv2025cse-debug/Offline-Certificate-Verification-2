package com.certificateverification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Entity representing a single block in the custom educational blockchain.
 * Day 3: Custom Java blockchain block containing certificate hash and cryptographic linkage.
 */
@Entity
@Table(name = "blockchain_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Block sequence number in the chain (0 for genesis) */
    @Column(name = "block_index", nullable = false)
    private int index;

    /** Timestamp of block creation in milliseconds */
    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    /** Certificate ID associated with this block */
    @Column(name = "certificate_id")
    private String certificateId;

    /** SHA-256 hash of the canonical certificate data */
    @Column(name = "certificate_hash", length = 64)
    private String certificateHash;

    /** SHA-256 hash of the preceding block in the chain */
    @Column(name = "previous_hash", length = 64, nullable = false)
    private String previousHash;

    /** SHA-256 hash of this block */
    @Column(name = "hash", length = 64)
    private String hash;

    /** Compatible column mapping for previous schema versions */
    @Column(name = "current_hash", length = 64)
    private String currentHash;

    /** Compatible column mapping for previous schema versions */
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    /**
     * Constructor for creating a block with core Day 3 fields.
     */
    public Block(int index, long timestamp, String certificateId, String certificateHash, String previousHash, String hash) {
        this.index = index;
        this.timestamp = timestamp;
        this.certificateId = certificateId;
        this.certificateHash = certificateHash;
        this.previousHash = previousHash;
        this.hash = hash;
        this.currentHash = hash;
        this.data = certificateHash != null ? certificateHash : "";
    }

    @PrePersist
    @PreUpdate
    protected void syncFields() {
        if (currentHash == null || currentHash.isEmpty()) {
            currentHash = hash;
        }
        if (hash == null || hash.isEmpty()) {
            hash = currentHash;
        }
        if (data == null || data.isEmpty()) {
            data = certificateHash != null ? certificateHash : "";
        }
    }

    /**
     * Helper to return a human-readable formatted timestamp.
     */
    public String getFormattedTimestamp() {
        if (timestamp <= 0) {
            return "N/A";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(FORMATTER);
    }

    // Compatibility getters/setters
    public Long getBlockIndex() {
        return (long) index;
    }

    public void setBlockIndex(Long blockIndex) {
        this.index = blockIndex != null ? blockIndex.intValue() : 0;
    }

    public String getCurrentHash() {
        return hash != null ? hash : currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
        if (this.hash == null) {
            this.hash = currentHash;
        }
    }

    public String getHash() {
        return hash != null ? hash : currentHash;
    }

    public void setHash(String hash) {
        this.hash = hash;
        this.currentHash = hash;
    }
}
