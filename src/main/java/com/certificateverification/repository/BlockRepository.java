package com.certificateverification.repository;

import com.certificateverification.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Block entity (blockchain storage).
 * Day 1: Interface declared - blockchain queries added in Day 2+.
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Find the latest block in the chain (highest block index).
     *
     * @return Optional containing the last block
     */
    Optional<Block> findTopByOrderByBlockIndexDesc();

    /**
     * Find a block by its SHA-256 hash.
     *
     * @param currentHash the hash of the block
     * @return Optional containing the block if found
     */
    Optional<Block> findByCurrentHash(String currentHash);
}
