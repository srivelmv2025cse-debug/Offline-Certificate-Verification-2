package com.certificateverification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for blockchain operations: adding blocks, verifying chain integrity.
 * Day 1: Placeholder service - full implementation in Day 2+.
 */
@Service
public class BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    /**
     * Placeholder: Add a new block to the local blockchain.
     * Full implementation in Day 2.
     *
     * @param data the data to store in the block
     */
    public void addBlock(String data) {
        logger.info("BlockchainService.addBlock() - placeholder for Day 2. Data: {}", data);
    }

    /**
     * Placeholder: Verify the integrity of the entire blockchain.
     * Full implementation in Day 2.
     *
     * @return true if the chain is valid (always true in placeholder)
     */
    public boolean isChainValid() {
        logger.info("BlockchainService.isChainValid() - placeholder for Day 2");
        return true;
    }

    /**
     * Placeholder: Get the hash of the last block in the chain.
     * Full implementation in Day 2.
     *
     * @return hash string (placeholder value)
     */
    public String getLastBlockHash() {
        logger.info("BlockchainService.getLastBlockHash() - placeholder for Day 2");
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }
}
