package com.certificateverification.service;

import com.certificateverification.model.AuditLog;
import com.certificateverification.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing the audit trail in the certificate verification system.
 * Day 7: Tracks certificate issuance, verification, failed verification, revocation,
 * QR verification, and blockchain validation.
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Record a new audit log entry.
     *
     * @param certificateId certificate identifier (or system tag like BLOCKCHAIN_LEDGER)
     * @param action        action performed (e.g., CERTIFICATE_ISSUANCE, VERIFIED, FAILED_VERIFICATION, REVOCATION, QR_VERIFICATION, BLOCKCHAIN_VALIDATION)
     * @param result        outcome (e.g., SUCCESS, FAILED, VALID, TAMPERED, REVOKED)
     * @param details       additional human-readable context
     * @param ipAddress     IP address of the caller
     * @return persisted AuditLog
     */
    public AuditLog log(String certificateId, String action, String result, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .certificateId(certificateId != null ? certificateId.trim() : "SYSTEM")
                    .action(action != null ? action.trim() : "UNKNOWN")
                    .result(result != null ? result.trim() : "UNKNOWN")
                    .details(details)
                    .ipAddress(ipAddress != null ? ipAddress.trim() : "127.0.0.1")
                    .timestamp(LocalDateTime.now())
                    .build();

            AuditLog saved = auditLogRepository.save(auditLog);
            logger.info("Audit logged [{}]: certId='{}', action='{}', result='{}'",
                    saved.getId(), saved.getCertificateId(), saved.getAction(), saved.getResult());
            return saved;
        } catch (Exception e) {
            logger.error("Failed to write audit log for certId='{}', action='{}': {}", certificateId, action, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieve all audit logs ordered by newest first.
     */
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Retrieve audit logs for a specific certificate ID.
     */
    public List<AuditLog> getLogsByCertificateId(String certificateId) {
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return getAllLogs();
        }
        return auditLogRepository.findByCertificateIdOrderByTimestampDesc(certificateId.trim());
    }

    /**
     * Retrieve audit logs filtered by action type.
     */
    public List<AuditLog> getLogsByAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return getAllLogs();
        }
        return auditLogRepository.findByActionOrderByTimestampDesc(action.trim());
    }

    /**
     * Retrieve audit logs filtered by certificate ID and action.
     */
    public List<AuditLog> getLogsByCertificateIdAndAction(String certificateId, String action) {
        if ((certificateId == null || certificateId.trim().isEmpty()) && (action == null || action.trim().isEmpty())) {
            return getAllLogs();
        }
        if (certificateId == null || certificateId.trim().isEmpty()) {
            return getLogsByAction(action);
        }
        if (action == null || action.trim().isEmpty()) {
            return getLogsByCertificateId(certificateId);
        }
        return auditLogRepository.findByCertificateIdAndActionOrderByTimestampDesc(certificateId.trim(), action.trim());
    }

    /**
     * Search audit logs by query (partial match on certificateId).
     */
    public List<AuditLog> searchLogs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllLogs();
        }
        return auditLogRepository.findByCertificateIdContainingIgnoreCaseOrderByTimestampDesc(query.trim());
    }

    /**
     * Get a specific audit log by ID.
     */
    public Optional<AuditLog> getLogById(Long id) {
        return auditLogRepository.findById(id);
    }
}
