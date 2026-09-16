package com.certificateverification.repository;

import com.certificateverification.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Certificate entity.
 * Provides CRUD operations and custom query methods.
 * Day 1: Interface declared - custom queries added in Day 2+.
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /**
     * Find a certificate by its unique certificate ID.
     *
     * @param certificateId the unique certificate identifier
     * @return Optional containing the certificate if found
     */
    Optional<Certificate> findByCertificateId(String certificateId);

    /**
     * Find a certificate by its blockchain hash.
     *
     * @param blockchainHash the SHA-256 hash stored on the blockchain
     * @return Optional containing the certificate if found
     */
    Optional<Certificate> findByBlockchainHash(String blockchainHash);

    /**
     * Check whether a certificate exists by its certificate ID.
     *
     * @param certificateId the unique certificate identifier
     * @return true if the certificate exists
     */
    boolean existsByCertificateId(String certificateId);

    /**
     * Retrieve all certificates ordered by creation date descending.
     *
     * @return list of certificates
     */
    java.util.List<Certificate> findAllByOrderByCreatedAtDesc();
}
