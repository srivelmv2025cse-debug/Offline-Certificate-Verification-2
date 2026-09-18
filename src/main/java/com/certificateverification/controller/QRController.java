package com.certificateverification.controller;

import com.certificateverification.model.Certificate;
import com.certificateverification.qr.QRService;
import com.certificateverification.qr.QRVerificationRequest;
import com.certificateverification.qr.QRVerificationResponse;
import com.certificateverification.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for QR code generation and verification.
 *
 * <p>Endpoints:
 * - POST /api/qr/generate/{certificateId}: Generate QR code for a certificate
 * - GET  /api/qr/{certificateId}: Retrieve QR code data and payload
 * - POST /api/qr/verify: Verify a certificate using QR value or manual ID
 * - POST /api/qr/decode: Decode a QR code image to text</p>
 */
@RestController
@RequestMapping("/api/qr")
public class QRController {

    private final QRService qrService;
    private final CertificateService certificateService;

    @Autowired
    public QRController(QRService qrService, CertificateService certificateService) {
        this.qrService = qrService;
        this.certificateService = certificateService;
    }

    /**
     * Generate or regenerate QR code for a certificate.
     * POST /api/qr/generate/{certificateId}
     *
     * @param certificateId certificate ID
     * @return 200 OK with QR data URI and payload
     */
    @PostMapping("/generate/{certificateId}")
    public ResponseEntity<?> generateQRCode(@PathVariable("certificateId") String certificateId) {
        try {
            Certificate cert = qrService.generateAndSaveQRCode(certificateId);
            String payload = qrService.getQRPayload(certificateId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "QR code generated successfully.");
            response.put("certificateId", cert.getCertificateId());
            response.put("qrCodeData", cert.getQrCodeData());
            response.put("qrPayload", payload);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error generating QR code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Retrieve QR code information for a certificate.
     * GET /api/qr/{certificateId}
     *
     * @param certificateId certificate ID
     * @return 200 OK with QR details
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getQRCode(@PathVariable("certificateId") String certificateId) {
        Optional<Certificate> certOpt = certificateService.getCertificateById(certificateId);
        if (certOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Certificate with ID '" + certificateId + "' not found"));
        }
        Certificate cert = certOpt.get();
        String qrData = cert.getQrCodeData();
        if (qrData == null || qrData.isEmpty()) {
            qrData = qrService.generateQRCodeForCertificate(cert);
            certificateService.generateQRCodeForCertificate(certificateId);
        }
        String payload = qrService.getQRPayload(certificateId);

        Map<String, Object> response = new HashMap<>();
        response.put("certificateId", cert.getCertificateId());
        response.put("qrCodeData", qrData);
        response.put("qrPayload", payload);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify a certificate using scanned QR code value or manual certificate ID.
     * POST /api/qr/verify
     *
     * @param request     QR verification request
     * @param httpRequest HTTP servlet request to capture IP
     * @return 200 OK with QRVerificationResponse
     */
    @PostMapping("/verify")
    public ResponseEntity<QRVerificationResponse> verifyQRCode(
            @RequestBody QRVerificationRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        QRVerificationResponse response = qrService.verifyQRCode(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Decode text from a Base64 QR code image.
     * POST /api/qr/decode
     */
    @PostMapping("/decode")
    public ResponseEntity<?> decodeQRCode(@RequestBody Map<String, String> body) {
        String imageBase64 = body.get("imageBase64");
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "imageBase64 is required"));
        }
        Optional<String> decoded = qrService.decodeQRCodeImage(imageBase64);
        if (decoded.isPresent()) {
            return ResponseEntity.ok(Map.of("decodedText", decoded.get()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No readable QR code found in the image"));
        }
    }
}
