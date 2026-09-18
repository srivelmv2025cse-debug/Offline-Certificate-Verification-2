package com.certificateverification.controller;

import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.Certificate;
import com.certificateverification.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for certificate operations: issue, verify, revoke.
 * Day 4: Implemented POST /api/certificates/verify with structured response & tamper detection.
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    @Autowired
    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Get all certificates.
     *
     * @return list of certificates
     */
    @GetMapping
    public ResponseEntity<List<Certificate>> getAllCertificates() {
        List<Certificate> certificates = certificateService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }

    /**
     * Get a specific certificate by its certificate ID.
     *
     * @param certificateId unique certificate identifier
     * @return certificate if found, or 404 Not Found
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificateById(@PathVariable("certificateId") String certificateId) {
        Optional<Certificate> certificate = certificateService.getCertificateById(certificateId);
        if (certificate.isPresent()) {
            return ResponseEntity.ok(certificate.get());
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Certificate with ID '" + certificateId + "' not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Issue a new certificate via REST API.
     *
     * @param certificate Certificate object from request body
     * @return 201 Created with saved certificate, or 400 Bad Request on error
     */
    @PostMapping
    public ResponseEntity<?> issueCertificate(@RequestBody Certificate certificate) {
        try {
            Certificate issued = certificateService.issueCertificate(certificate);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Certificate issued successfully.");
            response.put("certificate", issued);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Verify certificate data against the blockchain and detect tampering or revocation.
     * POST /api/certificates/verify
     *
     * @param request verification request body
     * @param httpRequest servlet request to extract client IP
     * @return 200 OK with CertificateVerificationResponse, or 400 Bad Request on validation error
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCertificate(
            @RequestBody CertificateVerificationRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
            CertificateVerificationResponse response = certificateService.verifyCertificate(request, ipAddress);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred during verification: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Revoke a certificate by its certificate ID.
     *
     * @param certificateId unique certificate identifier
     * @return 200 OK with revoked certificate, or 400 Bad Request
     */
    @PostMapping("/{certificateId}/revoke")
    public ResponseEntity<?> revokeCertificate(@PathVariable("certificateId") String certificateId) {
        try {
            Certificate revoked = certificateService.revokeCertificate(certificateId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Certificate revoked successfully.");
            response.put("certificate", revoked);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Health check endpoint for API status.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Certificate Verification API",
                "day", "4 - Certificate Verification & Fraud Detection"
        ));
    }
}
