package com.certificateverification.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Core blockchain implementation for the certificate verification system.
 * Manages the local chain of certificate transaction blocks.
 *
 * <p>Day 1: Placeholder - SHA-256 hashing, proof-of-work, and chain
 * validation will be implemented in Day 2.</p>
 */
@Component
public class BlockchainManager {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainManager.class);

    /**
     * Placeholder: Compute SHA-256 hash of input data.
     * Full implementation in Day 2.
     *
     * @param data the input string to hash
     * @return hex-encoded SHA-256 hash (placeholder)
     */
    public String computeHash(String data) {
        logger.debug("BlockchainManager.computeHash() - placeholder for Day 2");
        return "placeholder-hash";
    }

    /**
     * Placeholder: Mine a new block with proof-of-work.
     * Full implementation in Day 2.
     *
     * @param previousHash hash of the previous block
     * @param data         transaction data for the new block
     */
    public void mineBlock(String previousHash, String data) {
        logger.info("BlockchainManager.mineBlock() - placeholder for Day 2");
    }

    /**
     * Placeholder: Validate the entire local blockchain.
     * Full implementation in Day 2.
     *
     * @return true if chain is valid (always true in placeholder)
     */
    public boolean validateChain() {
        logger.info("BlockchainManager.validateChain() - placeholder for Day 2");
        return true;
    }
}
