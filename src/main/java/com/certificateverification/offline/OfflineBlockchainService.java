package com.certificateverification.offline;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.blockchain.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service managing the local offline blockchain cache.
 *
 * <p>Day 8: Stores the blockchain copy loaded from an offline synchronization package.
 * Performs cryptographic chain integrity verification and block lookup without
 * requiring an active database or network connection.</p>
 */
@Service
public class OfflineBlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(OfflineBlockchainService.class);

    private final List<OfflineBlock> chain = new ArrayList<>();
    private boolean loaded = false;

    /**
     * Load blockchain blocks from an offline synchronization package.
     *
     * @param syncPackage the package containing blockchain blocks
     */
    public synchronized void loadFromSyncPackage(OfflineSyncPackage syncPackage) {
        chain.clear();
        if (syncPackage != null && syncPackage.getBlockchainBlocks() != null) {
            chain.addAll(syncPackage.getBlockchainBlocks());
            loaded = true;
            logger.info("Loaded {} block(s) into offline blockchain cache", chain.size());
        } else {
            loaded = false;
            logger.warn("Sync package or blockchain blocks list was null");
        }
    }

    /**
     * Whether an offline blockchain snapshot has been loaded into memory.
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * Total number of blocks in the offline blockchain cache.
     */
    public synchronized int getChainSize() {
        return chain.size();
    }

    /**
     * Unmodifiable view of the offline blockchain.
     */
    public synchronized List<OfflineBlock> getChain() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    /**
     * Find a block in the offline blockchain cache by certificate ID.
     *
     * @param certificateId unique certificate identifier
     * @return optional containing the block if found
     */
    public synchronized Optional<OfflineBlock> getBlockByCertificateId(String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return Optional.empty();
        }
        String searchId = certificateId.trim();
        return chain.stream()
                .filter(b -> searchId.equalsIgnoreCase(b.getCertificateId()))
                .findFirst();
    }

    /**
     * Calculate SHA-256 hash of an offline block's contents.
     * Uses the identical formula as the online {@link Blockchain#calculateHash}:
     * {@code index:timestamp:certificateId:certificateHash:previousHash}
     */
    public String calculateHash(int index, long timestamp, String certificateId, String certificateHash, String previousHash) {
        String data = index + ":" + timestamp + ":" + certificateId + ":" + certificateHash + ":" + previousHash;
        return HashUtil.sha256(data);
    }

    /**
     * Calculate SHA-256 hash from an {@link OfflineBlock} object.
     */
    public String calculateHash(OfflineBlock block) {
        if (block == null) {
            return "";
        }
        return calculateHash(block.getIndex(), block.getTimestamp(), block.getCertificateId(),
                block.getCertificateHash(), block.getPreviousHash());
    }

    /**
     * Cryptographically validates the entire offline blockchain copy.
     *
     * <p>Checks:
     * 1. Genesis block has index 0 and expected 64-zero previous hash.
     * 2. Genesis block stored hash matches recalculated SHA-256 hash.
     * 3. Every block index is strictly sequential.
     * 4. Every block's stored hash matches its recalculated SHA-256 hash (tamper check).
     * 5. Every block's previousHash matches the preceding block's actual hash (chain linkage).</p>
     *
     * @return true if the offline blockchain is valid and untampered, false otherwise
     */
    public synchronized boolean isChainValid() {
        if (chain.isEmpty()) {
            logger.warn("Offline chain validation failed: cache is empty");
            return false;
        }

        // Validate Genesis block
        OfflineBlock genesis = chain.get(0);
        if (genesis.getIndex() != 0) {
            logger.warn("Offline chain validation failed: Genesis block index is {} (expected 0)", genesis.getIndex());
            return false;
        }
        if (!Blockchain.GENESIS_PREVIOUS_HASH.equals(genesis.getPreviousHash())) {
            logger.warn("Offline chain validation failed: Genesis previousHash mismatch");
            return false;
        }
        String genesisExpectedHash = calculateHash(genesis);
        if (!genesisExpectedHash.equals(genesis.getHash())) {
            logger.warn("Offline chain validation failed: Genesis hash tampered! Stored={}, Recalculated={}",
                    genesis.getHash(), genesisExpectedHash);
            return false;
        }

        // Validate subsequent blocks
        for (int i = 1; i < chain.size(); i++) {
            OfflineBlock current = chain.get(i);
            OfflineBlock previous = chain.get(i - 1);

            if (current.getIndex() != previous.getIndex() + 1) {
                logger.warn("Offline chain validation failed: Non-sequential block index at #{} (expected #{})",
                        current.getIndex(), previous.getIndex() + 1);
                return false;
            }

            String recalculatedHash = calculateHash(current);
            if (!recalculatedHash.equals(current.getHash())) {
                logger.warn("Offline chain validation failed: Block #{} content tampered! Stored={}, Recalculated={}",
                        current.getIndex(), current.getHash(), recalculatedHash);
                return false;
            }

            if (!current.getPreviousHash().equals(previous.getHash())) {
                logger.warn("Offline chain validation failed: Block #{} previousHash ({}) does not match Block #{} hash ({})",
                        current.getIndex(), current.getPreviousHash(), previous.getIndex(), previous.getHash());
                return false;
            }
        }

        return true;
    }

    /**
     * Clear the offline cache.
     */
    public synchronized void clear() {
        chain.clear();
        loaded = false;
        logger.info("Cleared offline blockchain cache");
    }
}
