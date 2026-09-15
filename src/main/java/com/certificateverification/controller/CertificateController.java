package com.certificateverification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API controller for certificate operations.
 * Day 1: Placeholder endpoints - to be implemented in Day 2+.
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    /**
     * Placeholder: Verify a certificate by its ID or hash.
     * To be fully implemented in Day 2.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyCertificate() {
        return ResponseEntity.ok(Map.of(
                "status", "placeholder",
                "message", "Certificate verification will be implemented in Day 2."
        ));
    }

    /**
     * Placeholder: Issue a new certificate.
     * To be fully implemented in Day 2.
     */
    @GetMapping("/issue")
    public ResponseEntity<Map<String, String>> issueCertificate() {
        return ResponseEntity.ok(Map.of(
                "status", "placeholder",
                "message", "Certificate issuance will be implemented in Day 2."
        ));
    }

    /**
     * Health check endpoint for API status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Certificate Verification API",
                "day", "1 - Project Setup"
        ));
    }
}
