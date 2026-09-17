package com.certificateverification.blockchain;

import com.certificateverification.model.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manager component exposing blockchain operations.
 * Delegates to the core Blockchain instance.
 */
@Component
public class BlockchainManager {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainManager.class);

    private final Blockchain blockchain;

    @Autowired
    public BlockchainManager(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    /**
     * Compute SHA-256 hash of input data using Java MessageDigest.
     *
     * @param data the input string to hash
     * @return hex-encoded SHA-256 hash
     */
    public String computeHash(String data) {
        return HashUtil.sha256(data);
    }

    /**
     * Add a new certificate block to the blockchain.
     *
     * @param certificateId certificate ID
     * @param certificateHash certificate SHA-256 hash
     * @return the created block
     */
    public Block addBlock(String certificateId, String certificateHash) {
        return blockchain.addCertificateBlock(certificateId, certificateHash);
    }

    /**
     * Validate the entire local blockchain.
     *
     * @return true if chain is valid and untampered
     */
    public boolean validateChain() {
        return blockchain.isChainValid();
    }

    /**
     * Get all blocks in the blockchain.
     */
    public List<Block> getChain() {
        return blockchain.getChain();
    }

    /**
     * Get the latest block.
     */
    public Block getLatestBlock() {
        return blockchain.getLatestBlock();
    }
}
