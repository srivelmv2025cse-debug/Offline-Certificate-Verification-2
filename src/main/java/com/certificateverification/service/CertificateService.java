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
import com.certificateverification.signature.DigitalSignatureService;
import com.certificateverification.signature.InstitutionKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for certificate operations: issue, verify, revoke.
 * Day 4: Implemented certificate verification and tamper/fraud detection.
 * Day 6: Integrated RSA digital signature on issuance and signature verification.
 */
@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final Blockchain blockchain;
    private final AuditLogRepository auditLogRepository;
    private final com.certificateverification.qr.QRService qrService;
    private final DigitalSignatureService digitalSignatureService;
    private final InstitutionKeyStore institutionKeyStore;

    @Autowired
    public CertificateService(CertificateRepository certificateRepository,
                              Blockchain blockchain,
                              AuditLogRepository auditLogRepository,
                              com.certificateverification.qr.QRService qrService,
                              DigitalSignatureService digitalSignatureService,
                              InstitutionKeyStore institutionKeyStore) {
        this.certificateRepository = certificateRepository;
        this.blockchain = blockchain;
        this.auditLogRepository = auditLogRepository;
        this.qrService = qrService;
        this.digitalSignatureService = digitalSignatureService;
        this.institutionKeyStore = institutionKeyStore;
    }

    /**
     * Issue a new certificate.
     * Day 6: After anchoring to blockchain, sign the certificate hash using the
     * institution's RSA private key and store the Base64 signature.
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

        // Day 3: Generate SHA-256 certificate hash using canonical format
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

        // Day 3: Anchor certificate hash to blockchain
        Block block = blockchain.addCertificateBlock(certificate.getCertificateId(), certificateHash);
        logger.info("Certificate {} anchored to Blockchain Block #{} with hash {}",
                certificate.getCertificateId(), block.getIndex(), block.getHash());

        // Day 6: Sign the certificate hash using the institution's RSA private key
        try {
            KeyPair keyPair = institutionKeyStore.getOrCreateKeyPair(certificate.getInstitutionName());
            String signature = digitalSignatureService.sign(certificateHash, keyPair.getPrivate());
            certificate.setDigitalSignature(signature);
            logger.info("Certificate {} digitally signed with RSA-2048 for institution '{}'",
                    certificate.getCertificateId(), certificate.getInstitutionName());
        } catch (Exception e) {
            // Signing failure is non-fatal for issuance; certificate is still anchored on blockchain.
            // Log a warning but continue so the certificate is persisted without a signature.
            logger.warn("Failed to generate digital signature for certificate {}: {}",
                    certificate.getCertificateId(), e.getMessage());
        }

        // Day 5: Generate QR code
        try {
            qrService.generateQRCodeForCertificate(certificate);
        } catch (Exception e) {
            logger.warn("Failed to generate QR code during issuance for certificate {}: {}",
                    certificate.getCertificateId(), e.getMessage());
        }

        Certificate saved = certificateRepository.save(certificate);
        logger.info("Certificate successfully issued with ID: {}", saved.getCertificateId());
        return saved;
    }

    /**
     * Generate or regenerate a QR code for a certificate.
     */
    public Certificate generateQRCodeForCertificate(String certificateId) {
        logger.info("Generating QR code for certificate: {}", certificateId);
        return qrService.generateAndSaveQRCode(certificateId);
    }

    /**
     * Retrieve a certificate by its unique certificate ID.
     */
    public Optional<Certificate> getCertificateById(String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return Optional.empty();
        }
        return certificateRepository.findByCertificateId(certificateId.trim());
    }

    /**
     * Retrieve all certificates sorted by creation date descending.
     */
    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Revoke a certificate by its unique certificate ID.
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
     * Core verification: blockchain integrity + digital signature + revocation check.
     *
     * <p>Day 4: hash matching, tamper detection, revocation.
     * <p>Day 6: A certificate is GENUINE only when ALL three conditions hold:
     * <ol>
     *   <li>Certificate hash matches the blockchain record (integrity)</li>
     *   <li>RSA digital signature is valid (authenticity)</li>
     *   <li>Certificate is not revoked (validity)</li>
     * </ol>
     *
     * @param request   verification request containing certificate fields
     * @param ipAddress client IP address for audit logging
     * @return structured verification response including signature details
     */
    public CertificateVerificationResponse verifyCertificate(CertificateVerificationRequest request, String ipAddress) {
        if (request == null) {
            throw new IllegalArgumentException("Verification request cannot be null");
        }

        String certificateId = request.getCertificateId() != null ? request.getCertificateId().trim() : "";
        String studentName   = request.getStudentName()   != null ? request.getStudentName().trim()   : "";
        String courseName    = request.getCourseName()    != null ? request.getCourseName().trim()    : "";
        String institutionName = request.getInstitutionName() != null ? request.getInstitutionName().trim() : "";
        String issueDate     = request.getIssueDate()     != null ? request.getIssueDate().trim()     : "";

        if (certificateId.isEmpty()) {
            throw new IllegalArgumentException("Certificate ID is required for verification");
        }

        logger.info("Verifying certificate ID: '{}' from IP: {}", certificateId, ipAddress);

        // 1. Search the blockchain for the certificate block
        Optional<Block> blockOpt = blockchain.getBlockByCertificateId(certificateId);

        // 2. Search database for existing certificate record
        Optional<Certificate> certOpt = certificateRepository.findByCertificateId(certificateId);

        // 3. Resolve certificateType and expiryDate for deterministic canonical hashing
        String certificateType = request.getCertificateType();
        if ((certificateType == null || certificateType.trim().isEmpty()) && certOpt.isPresent()) {
            certificateType = certOpt.get().getCertificateType();
        }
        if (certificateType == null) certificateType = "";

        String expiryDate = request.getExpiryDate();
        if ((expiryDate == null || expiryDate.trim().isEmpty()) && certOpt.isPresent()
                && certOpt.get().getExpiryDate() != null) {
            expiryDate = certOpt.get().getExpiryDate().toString();
        }
        if (expiryDate == null) expiryDate = "";

        // 4. Recalculate SHA-256 hash using the SAME canonical format as issuance
        String calculatedHash = HashUtil.hashCertificate(
                certificateId, studentName, courseName, institutionName,
                certificateType, issueDate, expiryDate
        );

        String blockchainHash = blockOpt.map(Block::getCertificateHash).orElse(null);
        boolean blockchainMatch = blockchainHash != null && blockchainHash.equalsIgnoreCase(calculatedHash);

        // -----------------------------------------------------------------------
        // Day 6: Digital Signature Verification
        // -----------------------------------------------------------------------
        boolean signatureValid     = false;
        String  signatureStatus    = "NOT SIGNED";
        String  signingInstitution = null;
        String  publicKeyFingerprint = null;

        if (certOpt.isPresent()) {
            Certificate dbCert = certOpt.get();
            String storedSignature = dbCert.getDigitalSignature();
            String institutionForKey = dbCert.getInstitutionName();

            if (storedSignature != null && !storedSignature.trim().isEmpty()) {
                signingInstitution = institutionForKey;
                // Retrieve the institution's public key from the in-memory key store
                PublicKey publicKey = institutionKeyStore.getPublicKey(institutionForKey);
                if (publicKey != null) {
                    // Verify the stored signature against the blockchain hash
                    // (i.e., the hash that was actually signed at issuance time)
                    String hashToVerify = dbCert.getBlockchainHash() != null
                            ? dbCert.getBlockchainHash()
                            : calculatedHash;
                    signatureValid = digitalSignatureService.verify(hashToVerify, storedSignature, publicKey);
                    signatureStatus = signatureValid ? "VALID" : "INVALID";
                    publicKeyFingerprint = digitalSignatureService.getPublicKeyFingerprint(publicKey);
                    logger.info("Digital signature verification for '{}': {} (institution: '{}')",
                            certificateId, signatureStatus, institutionForKey);
                } else {
                    signatureStatus = "KEY NOT FOUND";
                    logger.warn("No public key found in key store for institution '{}' — cannot verify signature",
                            institutionForKey);
                }
            } else {
                signatureStatus = "SIGNATURE MISSING";
                logger.info("No digital signature stored for certificate '{}' (pre-Day6 certificate)", certificateId);
            }
        }

        // -----------------------------------------------------------------------
        // 5. Verification verdict (Day 6 updated logic)
        //    A certificate is GENUINE only when:
        //    (a) hash matches blockchain  AND
        //    (b) digital signature is valid  AND
        //    (c) certificate is not revoked
        // -----------------------------------------------------------------------
        String result;
        String certificateStatus;
        boolean verified = false;
        String  auditAction;
        String  message;

        boolean isRevoked = certOpt.isPresent()
                && (certOpt.get().isRevoked() || "REVOKED".equalsIgnoreCase(certOpt.get().getStatus()));

        if (blockOpt.isEmpty()) {
            result            = "Certificate Not Found / Potentially Fake";
            certificateStatus = certOpt.map(Certificate::getStatus).orElse("NOT_FOUND");
            auditAction       = "FAILED_VERIFICATION";
            message           = "Certificate ID '" + certificateId + "' was not found on the blockchain ledger.";
        } else if (isRevoked) {
            result            = "REVOKED CERTIFICATE";
            certificateStatus = "REVOKED";
            auditAction       = "FAILED_VERIFICATION";
            message           = "Certificate has been formally revoked by the issuing authority.";
        } else if (!blockchainMatch) {
            result            = "TAMPERED CERTIFICATE";
            certificateStatus = "TAMPERED";
            auditAction       = "FAILED_VERIFICATION";
            message           = "Certificate data does not match the immutable hash recorded on the blockchain.";
        } else if (!signatureValid) {
            // Hash matches but signature is invalid/missing
            result            = "TAMPERED CERTIFICATE";
            certificateStatus = "TAMPERED";
            auditAction       = "FAILED_VERIFICATION";
            String sigReason  = "SIGNATURE MISSING".equals(signatureStatus)
                    ? "No digital signature is present on this certificate record."
                    : "Digital signature verification failed — the certificate may have been tampered with.";
            message = sigReason;
        } else {
            // All three checks passed: blockchain match + valid signature + not revoked
            result            = "GENUINE CERTIFICATE";
            certificateStatus = certOpt.map(Certificate::getStatus).orElse("ISSUED");
            verified          = true;
            auditAction       = "VERIFIED";
            message           = "Certificate is authentic — blockchain hash matches and digital signature is valid.";
        }

        // 6. Record audit log
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setCertificateId(certificateId);
            auditLog.setAction(auditAction);
            auditLog.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
            auditLog.setDetails("Result: " + result
                    + " | hashMatch=" + blockchainMatch
                    + " | sigValid=" + signatureValid
                    + " | sigStatus=" + signatureStatus
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
                // Day 6 signature fields
                .signatureValid(signatureValid)
                .signatureStatus(signatureStatus)
                .signingInstitution(signingInstitution)
                .publicKeyFingerprint(publicKeyFingerprint)
                .build();
    }
}
