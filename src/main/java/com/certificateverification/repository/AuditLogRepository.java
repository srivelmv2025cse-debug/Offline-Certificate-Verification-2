package com.certificateverification.repository;

import com.certificateverification.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AuditLog entity.
 * Day 1: Interface declared - queries added in Day 2+.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit log entries ordered by timestamp descending (newest first).
     */
    List<AuditLog> findAllByOrderByTimestampDesc();

    /**
     * Find all audit log entries for a given certificate ID.
     *
     * @param certificateId the certificate identifier
     * @return list of audit log entries, ordered by most recent
     */
    List<AuditLog> findByCertificateIdOrderByTimestampDesc(String certificateId);

    /**
     * Find audit logs matching certificate ID partially.
     */
    List<AuditLog> findByCertificateIdContainingIgnoreCaseOrderByTimestampDesc(String certificateId);

    /**
     * Find audit logs by action.
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs by action and certificate ID.
     */
    List<AuditLog> findByCertificateIdAndActionOrderByTimestampDesc(String certificateId, String action);
}
