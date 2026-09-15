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
     * Find all audit log entries for a given certificate ID.
     *
     * @param certificateId the certificate identifier
     * @return list of audit log entries, ordered by most recent
     */
    List<AuditLog> findByCertificateIdOrderByTimestampDesc(String certificateId);
}
