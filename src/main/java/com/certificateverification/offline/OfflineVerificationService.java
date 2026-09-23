package com.certificateverification.offline;

import com.certificateverification.blockchain.HashUtil;
import com.certificateverification.signature.DigitalSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for offline certificate verification.
 *
 * <p>Day 8: Performs certificate verification using EXCLUSIVELY local cached data:
 * <ul>
 *   <li>Local blockchain cache (via {@link OfflineBlockchainService})</li>
 *   <li>Local certificate snapshot repository</li>
 *   <li>Local institution public keys</li>
 * </ul>
 * Operates completely without Internet access or online database queries.</p>
 */
@Service
public class OfflineVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(OfflineVerificationService.class);

    private final OfflineBlockchainService offlineBlockchainService;
    private final DigitalSignatureService digitalSignatureService;

    private final Map<String, OfflineCertificateRecord> certificateCache = new ConcurrentHashMap<>();
    private final Map<String, String> publicKeyCache = new ConcurrentHashMap<>();

    @Autowired
    public OfflineVerificationService(OfflineBlockchainService offlineBlockchainService,
                                     DigitalSignatureService digitalSignatureService) {
        this.offlineBlockchainService = offlineBlockchainService;
        this.digitalSignatureService = digitalSignatureService;
    }

    /**
     * Load certificates and institution public keys from an offline synchronization package.
     * Also loads the blockchain blocks into {@link OfflineBlockchainService}.
     *
     * @param syncPackage the package containing all snapshot data
     */
    public synchronized void loadFromSyncPackage(OfflineSyncPackage syncPackage) {
        if (syncPackage == null) {
            logger.warn("Received null syncPackage in loadFromSyncPackage");
            return;
        }

        // 1. Load blockchain copy
        offlineBlockchainService.loadFromSyncPackage(syncPackage);

        // 2. Load certificates cache
        certificateCache.clear();
        if (syncPackage.getCertificates() != null) {
            for (OfflineCertificateRecord record : syncPackage.getCertificates()) {
                if (record != null && record.getCertificateId() != null) {
                    certificateCache.put(record.getCertificateId().trim().toLowerCase(), record);
                }
            }
            logger.info("Loaded {} certificate(s) into offline verification cache", certificateCache.size());
        }

        // 3. Load public keys cache
        publicKeyCache.clear();
        if (syncPackage.getPublicKeys() != null) {
            for (Map.Entry<String, String> entry : syncPackage.getPublicKeys().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    publicKeyCache.put(entry.getKey().trim().toLowerCase(), entry.getValue());
                }
            }
            logger.info("Loaded {} public key(s) into offline verification keystore", publicKeyCache.size());
        }
    }

    /**
     * Perform offline certificate verification using the certificate ID.
     * Looks up certificate details from the local snapshot cache.
     *
     * @param certificateId certificate identifier to verify
     * @return structured offline verification result
     */
    public OfflineVerificationResult verifyOffline(String certificateId) {
        return verifyOffline(certificateId, null, null, null, null, null, null);
    }

    /**
     * Perform offline certificate verification with optional field overrides.
     * Allows testing tamper detection by supplying alternative values for student name, etc.
     *
     * @param certificateId   unique certificate identifier
     * @param studentName     optional candidate student name (null to use cached value)
     * @param courseName      optional candidate course name (null to use cached value)
     * @param institutionName optional candidate institution name (null to use cached value)
     * @param certificateType optional candidate certificate type (null to use cached value)
     * @param issueDate       optional candidate issue date (null to use cached value)
     * @param expiryDate      optional candidate expiry date (null to use cached value)
     * @return structured offline verification result
     */
    public OfflineVerificationResult verifyOffline(String certificateId,
                                                  String studentName,
                                                  String courseName,
                                                  String institutionName,
                                                  String certificateType,
                                                  String issueDate,
                                                  String expiryDate) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return OfflineVerificationResult.builder()
                    .result("NOT_FOUND")
                    .message("Certificate ID is required for verification")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        String certId = certificateId.trim();
        logger.info("Performing offline verification for Certificate ID: '{}'", certId);

        // 1. Search local certificate cache
        OfflineCertificateRecord cert = certificateCache.get(certId.toLowerCase());

        // 2. Search local blockchain cache for the certificate block
        Optional<OfflineBlock> blockOpt = offlineBlockchainService.getBlockByCertificateId(certId);

        if (cert == null && blockOpt.isEmpty()) {
            return OfflineVerificationResult.builder()
                    .result("NOT_FOUND")
                    .certificateId(certId)
                    .message("Certificate ID '" + certId + "' was not found in the offline blockchain snapshot.")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        if (blockOpt.isEmpty()) {
            return OfflineVerificationResult.builder()
                    .result("NOT_FOUND")
                    .certificateId(certId)
                    .studentName(cert != null ? cert.getStudentName() : null)
                    .courseName(cert != null ? cert.getCourseName() : null)
                    .institutionName(cert != null ? cert.getInstitutionName() : null)
                    .certificateStatus(cert != null ? cert.getStatus() : "NOT_FOUND")
                    .message("Certificate record found in cache but no corresponding block exists in the offline blockchain ledger.")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        OfflineBlock block = blockOpt.get();

        // Resolve data fields (prefer explicit parameters, fallback to cached record)
        String effStudentName = studentName != null && !studentName.trim().isEmpty()
                ? studentName.trim() : (cert != null ? cert.getStudentName() : "");
        String effCourseName = courseName != null && !courseName.trim().isEmpty()
                ? courseName.trim() : (cert != null ? cert.getCourseName() : "");
        String effInstitution = institutionName != null && !institutionName.trim().isEmpty()
                ? institutionName.trim() : (cert != null ? cert.getInstitutionName() : "");
        String effType = certificateType != null && !certificateType.trim().isEmpty()
                ? certificateType.trim() : (cert != null ? cert.getCertificateType() : "");
        String effIssueDate = issueDate != null && !issueDate.trim().isEmpty()
                ? issueDate.trim() : (cert != null ? cert.getIssueDate() : "");
        String effExpiryDate = expiryDate != null && !expiryDate.trim().isEmpty()
                ? expiryDate.trim() : (cert != null ? cert.getExpiryDate() : "");

        // 3. Check Revocation status
        boolean isRevoked = cert != null && (cert.isRevoked() || "REVOKED".equalsIgnoreCase(cert.getStatus()));
        if (isRevoked) {
            String reason = cert.getRevocationReason();
            return OfflineVerificationResult.builder()
                    .result("REVOKED")
                    .certificateId(certId)
                    .studentName(effStudentName)
                    .courseName(effCourseName)
                    .institutionName(effInstitution)
                    .blockchainMatch(true)
                    .certificateStatus("REVOKED")
                    .message("Certificate has been formally revoked by the issuing authority."
                            + (reason != null && !reason.isEmpty() ? " Reason: " + reason : ""))
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // 4. Check Expiry status
        boolean isExpired = false;
        if (cert != null) {
            if ("EXPIRED".equalsIgnoreCase(cert.getStatus())) {
                isExpired = true;
            } else if (effExpiryDate != null && !effExpiryDate.isEmpty()) {
                try {
                    LocalDate expDate = LocalDate.parse(effExpiryDate);
                    if (expDate.isBefore(LocalDate.now())) {
                        isExpired = true;
                    }
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        if (isExpired) {
            return OfflineVerificationResult.builder()
                    .result("EXPIRED")
                    .certificateId(certId)
                    .studentName(effStudentName)
                    .courseName(effCourseName)
                    .institutionName(effInstitution)
                    .blockchainMatch(true)
                    .certificateStatus("EXPIRED")
                    .message("Certificate expired on " + effExpiryDate + ".")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // 5. Blockchain hash integrity check
        String recalculatedHash = HashUtil.hashCertificate(
                certId, effStudentName, effCourseName, effInstitution,
                effType, effIssueDate, effExpiryDate
        );
        boolean blockchainMatch = block.getCertificateHash() != null
                && block.getCertificateHash().equalsIgnoreCase(recalculatedHash);

        if (!blockchainMatch) {
            return OfflineVerificationResult.builder()
                    .result("TAMPERED")
                    .certificateId(certId)
                    .studentName(effStudentName)
                    .courseName(effCourseName)
                    .institutionName(effInstitution)
                    .blockchainMatch(false)
                    .certificateStatus("TAMPERED")
                    .message("Certificate data does not match the immutable hash recorded in the offline blockchain ledger.")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // 6. Digital signature verification using local public key
        boolean signatureValid = false;
        String signatureStatus = "NOT_SIGNED";

        if (cert != null && cert.getDigitalSignature() != null && !cert.getDigitalSignature().trim().isEmpty()) {
            String base64Sig = cert.getDigitalSignature().trim();
            String instKey = effInstitution.toLowerCase();
            String base64PublicKey = publicKeyCache.get(instKey);

            if (base64PublicKey != null) {
                try {
                    PublicKey publicKey = digitalSignatureService.decodePublicKey(base64PublicKey);
                    // Verify the signature against the recorded certificate hash
                    String hashToVerify = cert.getBlockchainHash() != null
                            ? cert.getBlockchainHash() : recalculatedHash;
                    signatureValid = digitalSignatureService.verify(hashToVerify, base64Sig, publicKey);
                    signatureStatus = signatureValid ? "VALID" : "INVALID";
                } catch (Exception e) {
                    logger.warn("Error decoding public key or verifying signature offline: {}", e.getMessage());
                    signatureStatus = "VERIFICATION_ERROR";
                }
            } else {
                signatureStatus = "KEY_NOT_FOUND";
                logger.warn("No public key found in offline keystore for institution '{}'", effInstitution);
            }
        } else {
            signatureStatus = "SIGNATURE_MISSING";
        }

        if (!signatureValid) {
            String sigMsg = "SIGNATURE_MISSING".equals(signatureStatus)
                    ? "No digital signature is present on this offline certificate record."
                    : ("KEY_NOT_FOUND".equals(signatureStatus)
                    ? "Institution public key not found in offline snapshot — cannot verify authenticity."
                    : "Digital signature verification failed against local public key — possible tampering.");

            return OfflineVerificationResult.builder()
                    .result("TAMPERED")
                    .certificateId(certId)
                    .studentName(effStudentName)
                    .courseName(effCourseName)
                    .institutionName(effInstitution)
                    .blockchainMatch(true)
                    .signatureValid(false)
                    .signatureStatus(signatureStatus)
                    .certificateStatus("TAMPERED")
                    .message(sigMsg)
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        // 7. All checks passed: GENUINE
        return OfflineVerificationResult.builder()
                .result("GENUINE")
                .certificateId(certId)
                .studentName(effStudentName)
                .courseName(effCourseName)
                .institutionName(effInstitution)
                .blockchainMatch(true)
                .signatureValid(true)
                .signatureStatus("VALID")
                .certificateStatus(cert != null && cert.getStatus() != null ? cert.getStatus() : "VALID")
                .message("Certificate is authentic and verified offline against local blockchain copy and institution public key.")
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get the count of certificates loaded in the offline cache.
     */
    public int getCertificateCacheSize() {
        return certificateCache.size();
    }

    /**
     * Get the count of public keys loaded in the offline cache.
     */
    public int getPublicKeyCacheSize() {
        return publicKeyCache.size();
    }

    /**
     * Retrieve an offline certificate record by certificate ID.
     */
    public Optional<OfflineCertificateRecord> getCertificateRecord(String certificateId) {
        if (certificateId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(certificateCache.get(certificateId.trim().toLowerCase()));
    }

    /**
     * Unmodifiable collection of all cached certificate records.
     */
    public Collection<OfflineCertificateRecord> getAllCertificateRecords() {
        return Collections.unmodifiableCollection(certificateCache.values());
    }
}
