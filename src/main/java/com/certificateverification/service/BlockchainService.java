package com.certificateverification.service;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.blockchain.HashUtil;
import com.certificateverification.model.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for blockchain operations: adding blocks, querying blocks,
 * and validating chain integrity.
 */
@Service
public class BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    private final Blockchain blockchain;

    @Autowired
    public BlockchainService(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    /**
     * Add a new certificate block to the local blockchain.
     *
     * @param certificateId  unique certificate ID
     * @param certificateHash SHA-256 hash of the canonical certificate
     * @return created Block
     */
    public Block addCertificateBlock(String certificateId, String certificateHash) {
        logger.info("BlockchainService: Adding block for certificate {}", certificateId);
        return blockchain.addCertificateBlock(certificateId, certificateHash);
    }

    /**
     * Add a block by hashing arbitrary data string.
     *
     * @param data arbitrary data
     * @return created Block
     */
    public Block addBlock(String data) {
        String hash = HashUtil.sha256(data);
        String certId = "DATA-" + System.currentTimeMillis();
        return blockchain.addCertificateBlock(certId, hash);
    }

    /**
     * Verify the integrity of the entire blockchain.
     *
     * @return true if the chain is valid and untampered
     */
    public boolean isChainValid() {
        return blockchain.isChainValid();
    }

    /**
     * Get the hash of the last block in the chain.
     *
     * @return hash string
     */
    public String getLastBlockHash() {
        return blockchain.getLatestBlock().getHash();
    }

    /**
     * Get all blocks in the blockchain.
     *
     * @return list of blocks in chronological order
     */
    public List<Block> getAllBlocks() {
        return blockchain.getChain();
    }

    /**
     * Total number of blocks.
     */
    public int getBlockCount() {
        return blockchain.getChainSize();
    }

    /**
     * Find a block for a given certificate.
     */
    public Optional<Block> getBlockByCertificateId(String certificateId) {
        return blockchain.getBlockByCertificateId(certificateId);
    }
}
