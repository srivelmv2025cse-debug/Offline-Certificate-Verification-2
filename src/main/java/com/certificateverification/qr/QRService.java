package com.certificateverification.qr;

import com.certificateverification.blockchain.Blockchain;
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
import java.util.Optional;

/**
 * Service for managing QR code generation and offline verification.
 *
 * <p>Encodes certificate identifiers and blockchain hashes into QR codes
 * without exposing sensitive student credentials. Verifies certificates
 * against the local blockchain and records audit events.</p>
 */
@Service
public class QRService {

    private static final Logger logger = LoggerFactory.getLogger(QRService.class);

    private final QRCodeGenerator qrCodeGenerator;
    private final Blockchain blockchain;
    private final CertificateRepository certificateRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public QRService(QRCodeGenerator qrCodeGenerator,
                     Blockchain blockchain,
                     CertificateRepository certificateRepository,
                     AuditLogRepository auditLogRepository) {
        this.qrCodeGenerator = qrCodeGenerator;
        this.blockchain = blockchain;
        this.certificateRepository = certificateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Generate QR code for a certificate and attach the Base64 data URI to the certificate.
     * Encodes only non-sensitive data (Certificate ID and blockchain hash).
     *
     * @param certificate certificate to generate QR code for
     * @return Base64 data URI of the QR code
     */
    public String generateQRCodeForCertificate(Certificate certificate) {
        if (certificate == null || certificate.getCertificateId() == null) {
            throw new IllegalArgumentException("Certificate and Certificate ID cannot be null");
        }
        String certId = certificate.getCertificateId().trim();
        String hash = certificate.getBlockchainHash() != null ? certificate.getBlockchainHash().trim() : "";

        // Fallback to blockchain query if certificate's blockchainHash field wasn't set yet
        if (hash.isEmpty()) {
            hash = blockchain.getBlockByCertificateId(certId)
                    .map(Block::getCertificateHash)
                    .orElse("");
        }

        String payload = QRVerificationPayload.buildPayloadString(certId, hash);
        String dataUri = qrCodeGenerator.generateQRCodeDataUri(payload);
        certificate.setQrCodeData(dataUri);

        logger.info("Generated QR code for certificate: {}", certId);
        return dataUri;
    }

    /**
     * Generate and persist QR code for an existing certificate.
     *
     * @param certificateId unique certificate identifier
     * @return updated certificate entity
     */
    public Certificate generateAndSaveQRCode(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate with ID '" + certificateId + "' not found"));
        generateQRCodeForCertificate(cert);
        return certificateRepository.save(cert);
    }

    /**
     * Get QR code payload string for a certificate.
     *
     * @param certificateId certificate ID
     * @return payload JSON string
     */
    public String getQRPayload(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate with ID '" + certificateId + "' not found"));
        return QRVerificationPayload.buildPayloadString(cert.getCertificateId(), cert.getBlockchainHash());
    }

    /**
     * Decode text from a Base64 image.
     */
    public Optional<String> decodeQRCodeImage(String base64Image) {
        return qrCodeGenerator.decodeQRCodeFromBase64(base64Image);
    }

    /**
     * Verify certificate using QR value or manual certificate ID.
     * Returns simple results: "Genuine", "Tampered", "Not Found", "Revoked".
     *
     * @param request   QR verification request
     * @param ipAddress client IP for audit logging
     * @return structured QRVerificationResponse
     */
    public QRVerificationResponse verifyQRCode(QRVerificationRequest request, String ipAddress) {
        if (request == null) {
            return buildErrorResponse("Invalid QR Code", "INVALID_QR", "Verification request is empty", null);
        }

        String rawInput = null;

        // 1. If base64 image was uploaded, decode it first
        if (request.getQrImageBase64() != null && !request.getQrImageBase64().trim().isEmpty()) {
            Optional<String> decoded = decodeQRCodeImage(request.getQrImageBase64());
            if (decoded.isPresent()) {
                rawInput = decoded.get();
            } else {
                return buildErrorResponse("Invalid QR Code", "INVALID_QR", "Could not detect or decode a valid QR code in the uploaded image", null);
            }
        }

        // 2. Otherwise use qrValue or manual certificateId
        if (rawInput == null) {
            if (request.getQrValue() != null && !request.getQrValue().trim().isEmpty()) {
                rawInput = request.getQrValue().trim();
            } else if (request.getCertificateId() != null && !request.getCertificateId().trim().isEmpty()) {
                rawInput = request.getCertificateId().trim();
            }
        }

        if (rawInput == null || rawInput.trim().isEmpty()) {
            return buildErrorResponse("Invalid QR Code", "INVALID_QR", "Please enter a Certificate ID or scan a QR code", null);
        }

        // 3. Parse QR payload
        QRVerificationPayload payload;
        try {
            payload = QRVerificationPayload.parse(rawInput);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to parse QR payload: {}", e.getMessage());
            logAudit("UNKNOWN", "FAILED_QR_VERIFICATION", "Invalid QR code payload: " + rawInput, ipAddress);
            return buildErrorResponse("Invalid QR Code", "INVALID_QR", e.getMessage(), null);
        }

        String certId = payload.getCertId();
        String qrRef = payload.getRef();

        logger.info("Performing QR verification for certificate '{}' (ref: {}) from IP: {}", certId, qrRef, ipAddress);

        // 4. Search local blockchain and database
        Optional<Block> blockOpt = blockchain.getBlockByCertificateId(certId);
        Optional<Certificate> certOpt = certificateRepository.findByCertificateId(certId);

        String result;
        String status;
        boolean verified = false;
        boolean blockchainMatch = false;
        String blockchainHash = blockOpt.map(Block::getCertificateHash).orElse(null);
        String message;

        boolean isRevoked = certOpt.isPresent() && (certOpt.get().isRevoked() || "REVOKED".equalsIgnoreCase(certOpt.get().getStatus()));

        // Verification logic
        if (blockOpt.isEmpty()) {
            result = "Not Found";
            status = "NOT_FOUND";
            message = "Certificate ID '" + certId + "' was not found on the blockchain ledger.";
        } else if (isRevoked) {
            result = "Revoked";
            status = "REVOKED";
            blockchainMatch = true;
            message = "Certificate has been formally revoked by the issuing authority.";
        } else {
            // Check for tampering:
            // If QR reference was provided, it MUST match the blockchain hash
            boolean qrRefMismatch = (qrRef != null && !qrRef.trim().isEmpty() && !qrRef.trim().equalsIgnoreCase(blockchainHash));
            
            // If DB record exists, its recorded blockchainHash must match the blockchain block
            boolean dbMismatch = (certOpt.isPresent() && certOpt.get().getBlockchainHash() != null &&
                    !certOpt.get().getBlockchainHash().equalsIgnoreCase(blockchainHash));

            if (qrRefMismatch || dbMismatch) {
                result = "Tampered";
                status = "TAMPERED";
                blockchainMatch = false;
                message = "Tampering detected! The QR verification reference does not match the immutable blockchain ledger.";
            } else {
                result = "Genuine";
                status = "GENUINE";
                verified = true;
                blockchainMatch = true;
                message = "Certificate verified as genuine against the local blockchain.";
            }
        }

        // Log audit event
        String auditAction = verified ? "VERIFIED_QR" : "FAILED_QR_VERIFICATION";
        String auditResult = verified ? "SUCCESS" : "FAILED";
        logAudit(certId, auditAction, auditResult, "Result: " + result + " | ref=" + qrRef + " | blockHash=" + blockchainHash, ipAddress);

        return QRVerificationResponse.builder()
                .result(result)
                .status(status)
                .certificateId(certId)
                .student(certOpt.map(Certificate::getStudentName).orElse(null))
                .institution(certOpt.map(Certificate::getInstitutionName).orElse(null))
                .course(certOpt.map(Certificate::getCourseName).orElse(null))
                .issueDate(certOpt.map(c -> c.getIssueDate() != null ? c.getIssueDate().toString() : null).orElse(null))
                .blockchainHash(blockchainHash)
                .qrReference(qrRef)
                .blockchainMatch(blockchainMatch)
                .verified(verified)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private QRVerificationResponse buildErrorResponse(String result, String status, String message, String certId) {
        return QRVerificationResponse.builder()
                .result(result)
                .status(status)
                .certificateId(certId)
                .verified(false)
                .blockchainMatch(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void logAudit(String certificateId, String action, String details, String ipAddress) {
        String defaultResult = action != null && action.startsWith("VERIFIED") ? "SUCCESS" : "FAILED";
        logAudit(certificateId, action, defaultResult, details, ipAddress);
    }

    private void logAudit(String certificateId, String action, String result, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setCertificateId(certificateId);
            auditLog.setAction(action);
            auditLog.setResult(result);
            auditLog.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
            auditLog.setDetails(details);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to write QR audit log", e);
        }
    }
}
