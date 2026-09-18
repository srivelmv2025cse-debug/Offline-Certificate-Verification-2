package com.certificateverification.service;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.blockchain.HashUtil;
import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.AuditLog;
import com.certificateverification.model.Block;
import com.certificateverification.model.Certificate;
import com.certificateverification.repository.AuditLogRepository;
import com.certificateverification.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for certificate operations: issue, verify, revoke.
 * Day 4: Implemented certificate verification and tamper/fraud detection.
 */
@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final Blockchain blockchain;
    private final AuditLogRepository auditLogRepository;
    private final com.certificateverification.qr.QRService qrService;

    @Autowired
    public CertificateService(CertificateRepository certificateRepository,
                              Blockchain blockchain,
                              AuditLogRepository auditLogRepository,
                              com.certificateverification.qr.QRService qrService) {
        this.certificateRepository = certificateRepository;
        this.blockchain = blockchain;
        this.auditLogRepository = auditLogRepository;
        this.qrService = qrService;
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
        String certificateHash = HashUtil.hashCertificate(
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
        Block block = blockchain.addCertificateBlock(certificate.getCertificateId(), certificateHash);
        logger.info("Certificate {} anchored to Blockchain Block #{} with hash {}",
                certificate.getCertificateId(), block.getIndex(), block.getHash());

        // Day 5: Generate QR code encoding non-sensitive credentials (Certificate ID and verification reference)
        try {
            qrService.generateQRCodeForCertificate(certificate);
        } catch (Exception e) {
            logger.warn("Failed to generate QR code during issuance for certificate {}: {}", certificate.getCertificateId(), e.getMessage());
        }

        Certificate saved = certificateRepository.save(certificate);
        logger.info("Certificate successfully issued with ID: {}", saved.getCertificateId());
        return saved;
    }

    /**
     * Generate or regenerate a QR code for a certificate.
     *
     * @param certificateId certificate ID
     * @return updated certificate
     */
    public Certificate generateQRCodeForCertificate(String certificateId) {
        logger.info("Generating QR code for certificate: {}", certificateId);
        return qrService.generateAndSaveQRCode(certificateId);
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
    public List<Certificate> getAllCertificates() {
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
     * Legacy boolean verification check.
     */
    public boolean verifyCertificate(String certificateId) {
        logger.info("CertificateService.verifyCertificate({})", certificateId);
        return getCertificateById(certificateId).isPresent();
    }

    /**
     * Overload helper for verifying certificate with default IP.
     */
    public CertificateVerificationResponse verifyCertificate(CertificateVerificationRequest request) {
        return verifyCertificate(request, "127.0.0.1");
    }

    /**
     * Core Day 4 verification and tamper/fraud detection.
     *
     * <p>Generates SHA-256 hash using the same canonical format as issuance, searches
     * the blockchain for the certificate block, verifies authenticity, detects tampering,
     * checks revocation status, and logs the attempt to audit logs.</p>
     *
     * @param request verification request containing certificate fields
     * @param ipAddress client IP address for audit logging
     * @return structured verification response
     */
    public CertificateVerificationResponse verifyCertificate(CertificateVerificationRequest request, String ipAddress) {
        if (request == null) {
            throw new IllegalArgumentException("Verification request cannot be null");
        }

        String certificateId = request.getCertificateId() != null ? request.getCertificateId().trim() : "";
        String studentName = request.getStudentName() != null ? request.getStudentName().trim() : "";
        String courseName = request.getCourseName() != null ? request.getCourseName().trim() : "";
        String institutionName = request.getInstitutionName() != null ? request.getInstitutionName().trim() : "";
        String issueDate = request.getIssueDate() != null ? request.getIssueDate().trim() : "";

        if (certificateId.isEmpty()) {
            throw new IllegalArgumentException("Certificate ID is required for verification");
        }

        logger.info("Verifying certificate ID: '{}' from IP: {}", certificateId, ipAddress);

        // 1. Search the blockchain for the certificate block
        Optional<Block> blockOpt = blockchain.getBlockByCertificateId(certificateId);

        // 2. Search database for existing certificate
        Optional<Certificate> certOpt = certificateRepository.findByCertificateId(certificateId);

        // 3. Resolve certificateType and expiryDate for deterministic canonical hashing
        String certificateType = request.getCertificateType();
        if ((certificateType == null || certificateType.trim().isEmpty()) && certOpt.isPresent()) {
            certificateType = certOpt.get().getCertificateType();
        }
        if (certificateType == null) {
            certificateType = "";
        }

        String expiryDate = request.getExpiryDate();
        if ((expiryDate == null || expiryDate.trim().isEmpty()) && certOpt.isPresent() && certOpt.get().getExpiryDate() != null) {
            expiryDate = certOpt.get().getExpiryDate().toString();
        }
        if (expiryDate == null) {
            expiryDate = "";
        }

        // 4. Recalculate SHA-256 hash using the SAME canonical format used during issuance
        String calculatedHash = HashUtil.hashCertificate(
                certificateId,
                studentName,
                courseName,
                institutionName,
                certificateType,
                issueDate,
                expiryDate
        );

        String blockchainHash = blockOpt.map(Block::getCertificateHash).orElse(null);
        boolean blockchainMatch = blockchainHash != null && blockchainHash.equalsIgnoreCase(calculatedHash);

        // 5. Verification logic:
        // 1. Certificate ID not found: Result = "Certificate Not Found / Potentially Fake"
        // 2. Certificate ID found and hash matches: Result = "GENUINE CERTIFICATE"
        // 3. Certificate ID found but hash does not match: Result = "TAMPERED CERTIFICATE"
        // 4. Certificate is revoked: Result = "REVOKED CERTIFICATE"

        String result;
        String certificateStatus;
        boolean verified = false;
        String auditAction;
        String message;

        boolean isRevoked = certOpt.isPresent() && (certOpt.get().isRevoked() || "REVOKED".equalsIgnoreCase(certOpt.get().getStatus()));

        if (blockOpt.isEmpty()) {
            result = "Certificate Not Found / Potentially Fake";
            certificateStatus = certOpt.map(Certificate::getStatus).orElse("NOT_FOUND");
            auditAction = "FAILED_VERIFICATION";
            message = "Certificate ID '" + certificateId + "' was not found on the blockchain ledger.";
        } else if (isRevoked) {
            result = "REVOKED CERTIFICATE";
            certificateStatus = "REVOKED";
            auditAction = "FAILED_VERIFICATION";
            message = "Certificate has been formally revoked by the issuing authority.";
        } else if (blockchainMatch) {
            result = "GENUINE CERTIFICATE";
            certificateStatus = certOpt.map(Certificate::getStatus).orElse("ISSUED");
            verified = true;
            auditAction = "VERIFIED";
            message = "Certificate is authentic and matches the blockchain ledger.";
        } else {
            result = "TAMPERED CERTIFICATE";
            certificateStatus = "TAMPERED";
            auditAction = "FAILED_VERIFICATION";
            message = "Certificate data does not match the immutable hash recorded on the blockchain.";
        }

        // 6. Record audit log entry for every verification attempt
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setCertificateId(certificateId);
            auditLog.setAction(auditAction);
            auditLog.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
            auditLog.setDetails("Result: " + result + " | match=" + blockchainMatch
                    + " | calcHash=" + calculatedHash
                    + " | blockHash=" + (blockchainHash != null ? blockchainHash : "NONE"));
            auditLog.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(auditLog);
            logger.info("Verification audit logged: action={}, result={}, certId={}", auditAction, result, certificateId);
        } catch (Exception e) {
            logger.error("Failed to persist verification audit log", e);
        }

        return CertificateVerificationResponse.builder()
                .result(result)
                .certificateId(certificateId)
                .student(studentName)
                .institution(institutionName)
                .course(courseName)
                .issueDate(issueDate)
                .blockchainHash(blockchainHash != null ? blockchainHash : "N/A")
                .calculatedHash(calculatedHash)
                .blockchainMatch(blockchainMatch)
                .certificateStatus(certificateStatus)
                .verified(verified)
                .message(message)
                .build();
    }
}
