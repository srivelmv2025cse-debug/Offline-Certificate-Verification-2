package com.certificateverification.controller;

import com.certificateverification.model.AuditLog;
import com.certificateverification.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for audit log operations.
 * Day 7: Endpoints for retrieving audit trail records and filtering by certificateId or action.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieve audit logs with optional filters for certificateId, action, or general query.
     *
     * @param certificateId optional certificate identifier to filter by
     * @param action        optional action type to filter by
     * @param query         optional search query on certificate identifier
     * @return 200 OK with list of audit logs
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(value = "certificateId", required = false) String certificateId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "query", required = false) String query) {

        List<AuditLog> logs;
        if (query != null && !query.trim().isEmpty()) {
            logs = auditLogService.searchLogs(query.trim());
        } else if ((certificateId != null && !certificateId.trim().isEmpty()) || (action != null && !action.trim().isEmpty())) {
            logs = auditLogService.getLogsByCertificateIdAndAction(certificateId, action);
        } else {
            logs = auditLogService.getAllLogs();
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * Retrieve a specific audit log by its ID.
     *
     * @param id log ID
     * @return 200 OK with log if found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAuditLogById(@PathVariable("id") Long id) {
        Optional<AuditLog> logOpt = auditLogService.getLogById(id);
        if (logOpt.isPresent()) {
            return ResponseEntity.ok(logOpt.get());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Audit log with ID " + id + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Retrieve all audit logs for a specific certificate ID.
     *
     * @param certificateId certificate ID
     * @return 200 OK with list of logs
     */
    @GetMapping("/certificate/{certificateId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsForCertificate(@PathVariable("certificateId") String certificateId) {
        List<AuditLog> logs = auditLogService.getLogsByCertificateId(certificateId);
        return ResponseEntity.ok(logs);
    }
}
