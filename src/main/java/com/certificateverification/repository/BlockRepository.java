package com.certificateverification.repository;

import com.certificateverification.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Block entity (blockchain storage).
 * Day 3: Custom blockchain block query methods.
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Find the latest block in the chain (highest block index).
     *
     * @return Optional containing the last block
     */
    Optional<Block> findTopByOrderByIndexDesc();

    /**
     * Find all blocks in the chain ordered by index ascending.
     *
     * @return list of blocks in chronological order
     */
    List<Block> findAllByOrderByIndexAsc();

    /**
     * Find a block by its SHA-256 hash.
     *
     * @param hash the hash of the block
     * @return Optional containing the block if found
     */
    Optional<Block> findByHash(String hash);

    /**
     * Find a block by its associated certificate ID.
     *
     * @param certificateId certificate identifier
     * @return Optional containing the block if found
     */
    Optional<Block> findByCertificateId(String certificateId);
}
