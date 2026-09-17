package com.certificateverification.blockchain;

import com.certificateverification.model.Block;
import com.certificateverification.repository.BlockRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Custom in-memory and persistent educational blockchain completely in Java.
 *
 * <p>Stores blocks, generates genesis block, calculates SHA-256 hashes using
 * Java MessageDigest, links previous hashes, and validates integrity to detect
 * any tampered/modified blocks.</p>
 */
@Component
public class Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);

    public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);
    public static final String GENESIS_CERTIFICATE_ID = "GENESIS_BLOCK";
    public static final String GENESIS_DATA = "GENESIS_CERTIFICATE_DATA_ROOT";

    private final List<Block> chain = new ArrayList<>();
    private final BlockRepository blockRepository;

    @Autowired
    public Blockchain(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    /**
     * Initialize the blockchain: load existing blocks from SQLite, or create the Genesis block.
     */
    @PostConstruct
    public synchronized void init() {
        chain.clear();
        List<Block> storedBlocks = blockRepository.findAllByOrderByIndexAsc();

        if (storedBlocks.isEmpty()) {
            logger.info("No existing blocks found in database. Generating Genesis block...");
            Block genesis = createGenesisBlock();
            Block savedGenesis = blockRepository.save(genesis);
            chain.add(savedGenesis);
            logger.info("Genesis block created: Index={}, Hash={}", savedGenesis.getIndex(), savedGenesis.getHash());
        } else {
            logger.info("Loaded {} existing block(s) from database into blockchain", storedBlocks.size());
            chain.addAll(storedBlocks);
        }
    }

    /**
     * Create the Genesis block (Block #0).
     *
     * @return the genesis block
     */
    public Block createGenesisBlock() {
        int index = 0;
        long timestamp = 1704067200000L; // Fixed epoch: 2024-01-01 00:00:00 UTC
        String certificateId = GENESIS_CERTIFICATE_ID;
        String certificateHash = HashUtil.sha256(GENESIS_DATA);
        String previousHash = GENESIS_PREVIOUS_HASH;
        String hash = calculateHash(index, timestamp, certificateId, certificateHash, previousHash);

        return new Block(index, timestamp, certificateId, certificateHash, previousHash, hash);
    }

    /**
     * Calculate SHA-256 hash of block contents using Java MessageDigest.
     *
     * @param index           block index
     * @param timestamp       block timestamp
     * @param certificateId   certificate ID
     * @param certificateHash certificate SHA-256 hash
     * @param previousHash    previous block's SHA-256 hash
     * @return 64-character SHA-256 hash
     */
    public String calculateHash(int index, long timestamp, String certificateId, String certificateHash, String previousHash) {
        String data = index + ":" + timestamp + ":" + certificateId + ":" + certificateHash + ":" + previousHash;
        return HashUtil.sha256(data);
    }

    /**
     * Overloaded helper to calculate hash from a Block object.
     */
    public String calculateHash(Block block) {
        if (block == null) {
            return "";
        }
        return calculateHash(block.getIndex(), block.getTimestamp(), block.getCertificateId(), block.getCertificateHash(), block.getPreviousHash());
    }

    /**
     * Add a new certificate block to the blockchain.
     * Links the new block to the preceding block's hash.
     *
     * @param certificateId   unique certificate identifier
     * @param certificateHash SHA-256 hash of the canonical certificate data
     * @return the created and saved block
     */
    public synchronized Block addCertificateBlock(String certificateId, String certificateHash) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate ID is required to create a block");
        }
        if (certificateHash == null || certificateHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate hash is required to create a block");
        }

        if (chain.isEmpty()) {
            init();
        }

        Block previousBlock = getLatestBlock();
        int newIndex = previousBlock.getIndex() + 1;
        long timestamp = System.currentTimeMillis();
        String previousHash = previousBlock.getHash();
        String hash = calculateHash(newIndex, timestamp, certificateId.trim(), certificateHash.trim(), previousHash);

        Block newBlock = new Block(newIndex, timestamp, certificateId.trim(), certificateHash.trim(), previousHash, hash);
        Block savedBlock = blockRepository.save(newBlock);
        chain.add(savedBlock);

        logger.info("Successfully added Block #{} [cert: {}] with hash: {}", newIndex, certificateId, hash);
        return savedBlock;
    }

    /**
     * Get the latest block in the chain.
     */
    public synchronized Block getLatestBlock() {
        if (chain.isEmpty()) {
            init();
        }
        return chain.get(chain.size() - 1);
    }

    /**
     * Get an unmodifiable copy of all blocks in the chain.
     */
    public synchronized List<Block> getChain() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    /**
     * Total number of blocks in the chain.
     */
    public synchronized int getChainSize() {
        return chain.size();
    }

    /**
     * Find a block by certificate ID.
     */
    public synchronized Optional<Block> getBlockByCertificateId(String certificateId) {
        if (certificateId == null) {
            return Optional.empty();
        }
        return chain.stream()
                .filter(b -> certificateId.trim().equalsIgnoreCase(b.getCertificateId()))
                .findFirst();
    }

    /**
     * Validates the complete blockchain and detects any modified or tampered blocks.
     *
     * <p>Checks:
     * 1. Genesis block has valid index, hash, and structure.
     * 2. Every block's stored hash equals its recalculated SHA-256 hash (detects content tampering).
     * 3. Every block's previousHash matches the preceding block's actual hash (detects chain breaking / deletion).
     * 4. Sequential block indexes.</p>
     *
     * @return true if the entire blockchain is valid, false if tampered
     */
    public synchronized boolean isChainValid() {
        if (chain.isEmpty()) {
            logger.warn("Validation failed: Blockchain is empty");
            return false;
        }

        // Validate Genesis block
        Block genesis = chain.get(0);
        if (genesis.getIndex() != 0) {
            logger.warn("Validation failed: Genesis block index is not 0 (got {})", genesis.getIndex());
            return false;
        }
        if (!genesis.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
            logger.warn("Validation failed: Genesis previous hash does not match expected");
            return false;
        }
        String genesisExpectedHash = calculateHash(genesis);
        if (!genesis.getHash().equals(genesisExpectedHash)) {
            logger.warn("Validation failed: Genesis block hash tampered! Stored: {}, Expected: {}",
                    genesis.getHash(), genesisExpectedHash);
            return false;
        }

        // Validate subsequent blocks
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            // Check sequential indexing
            if (current.getIndex() != previous.getIndex() + 1) {
                logger.warn("Validation failed: Non-sequential block index at #{} (expected #{})",
                        current.getIndex(), previous.getIndex() + 1);
                return false;
            }

            // Check if current block data was tampered
            String recalculatedHash = calculateHash(current);
            if (!current.getHash().equals(recalculatedHash)) {
                logger.warn("Validation failed: Block #{} content tampered! Stored: {}, Recalculated: {}",
                        current.getIndex(), current.getHash(), recalculatedHash);
                return false;
            }

            // Check if link to previous block is intact
            if (!current.getPreviousHash().equals(previous.getHash())) {
                logger.warn("Validation failed: Block #{} previousHash ({}) does not match Block #{} hash ({})",
                        current.getIndex(), current.getPreviousHash(), previous.getIndex(), previous.getHash());
                return false;
            }
        }

        return true;
    }
}
