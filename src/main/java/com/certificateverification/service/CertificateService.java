package com.certificateverification.service;

import com.certificateverification.model.Certificate;
import com.certificateverification.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for certificate operations: issue, verify, revoke.
 * Day 1: Placeholder service - full implementation in Day 2+.
 */
@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final com.certificateverification.blockchain.Blockchain blockchain;

    @Autowired
    public CertificateService(CertificateRepository certificateRepository,
                              com.certificateverification.blockchain.Blockchain blockchain) {
        this.certificateRepository = certificateRepository;
        this.blockchain = blockchain;
    }

    /**
     * Issue a new certificate.
     * Validates and saves the certificate to the database.
     */
    public Certificate issueCertificate(Certificate certificate) {
        logger.info("Issuing new certificate: {}", certificate != null ? certificate.getCertificateId() : "null");
        
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate details cannot be null");
        }

        // Basic validations
        if (certificate.getCertificateId() == null || certificate.getCertificateId().trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate ID is required");
        }
        if (certificate.getStudentName() == null || certificate.getStudentName().trim().isEmpty()) {
            throw new IllegalArgumentException("Student Name is required");
        }
        if (certificate.getCourseName() == null || certificate.getCourseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Course Name is required");
        }
        if (certificate.getInstitutionName() == null || certificate.getInstitutionName().trim().isEmpty()) {
            throw new IllegalArgumentException("Institution Name is required");
        }
        if (certificate.getCertificateType() == null || certificate.getCertificateType().trim().isEmpty()) {
            throw new IllegalArgumentException("Certificate Type is required");
        }
        if (certificate.getIssueDate() == null) {
            throw new IllegalArgumentException("Issue Date is required");
        }
        if (certificate.getExpiryDate() != null && certificate.getExpiryDate().isBefore(certificate.getIssueDate())) {
            throw new IllegalArgumentException("Expiry Date must not be before Issue Date");
        }

        String trimmedCertId = certificate.getCertificateId().trim();
        if (certificateRepository.existsByCertificateId(trimmedCertId)) {
            throw new IllegalArgumentException("Certificate with ID '" + trimmedCertId + "' already exists");
        }

        // Clean values
        certificate.setCertificateId(trimmedCertId);
        certificate.setStudentName(certificate.getStudentName().trim());
        certificate.setCourseName(certificate.getCourseName().trim());
        certificate.setInstitutionName(certificate.getInstitutionName().trim());
        certificate.setCertificateType(certificate.getCertificateType().trim());

        // Set default status if not provided
        if (certificate.getStatus() == null || certificate.getStatus().trim().isEmpty()) {
            certificate.setStatus("ISSUED");
        }
        certificate.setRevoked(false);

        // Day 3: Custom blockchain integration
        // 1. Generate canonical representation of certificate data
        // 2. Generate SHA-256 certificate hash using Java MessageDigest
        String expiryStr = certificate.getExpiryDate() != null ? certificate.getExpiryDate().toString() : "";
        String certificateHash = com.certificateverification.blockchain.HashUtil.hashCertificate(
                certificate.getCertificateId(),
                certificate.getStudentName(),
                certificate.getCourseName(),
                certificate.getInstitutionName(),
                certificate.getCertificateType(),
                certificate.getIssueDate().toString(),
                expiryStr
        );
        certificate.setBlockchainHash(certificateHash);

        // 3 & 4 & 5. Create a blockchain block, store certificate hash, and link to previous block
        com.certificateverification.model.Block block = blockchain.addCertificateBlock(certificate.getCertificateId(), certificateHash);
        logger.info("Certificate {} anchored to Blockchain Block #{} with hash {}",
                certificate.getCertificateId(), block.getIndex(), block.getHash());

        Certificate saved = certificateRepository.save(certificate);
        logger.info("Certificate successfully issued with ID: {}", saved.getCertificateId());
        return saved;
    }
    
    /**
     * Retrieve a certificate by its unique certificate ID.
     *
     * @param certificateId the unique certificate identifier
     * @return Optional containing the certificate if found
     */
    public Optional<Certificate> getCertificateById(String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return Optional.empty();
        }
        return certificateRepository.findByCertificateId(certificateId.trim());
    }

    /**
     * Retrieve all certificates sorted by creation date descending.
     *
     * @return list of all certificates
     */
    public java.util.List<Certificate> getAllCertificates() {
        return certificateRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Revoke a certificate by its unique certificate ID.
     *
     * @param certificateId the unique certificate identifier
     * @return the updated certificate
     */
    public Certificate revokeCertificate(String certificateId) {
        logger.info("Revoking certificate: {}", certificateId);
        Certificate cert = getCertificateById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate with ID '" + certificateId + "' not found"));
        cert.setStatus("REVOKED");
        cert.setRevoked(true);
        return certificateRepository.save(cert);
    }

    /**
     * Verify a certificate by its ID.
     * (Placeholder for future verification tasks)
     */
    public boolean verifyCertificate(String certificateId) {
        logger.info("CertificateService.verifyCertificate({})", certificateId);
        return getCertificateById(certificateId).isPresent();
    }
}
