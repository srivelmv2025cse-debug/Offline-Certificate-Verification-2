package com.certificateverification;

import com.certificateverification.model.AuditLog;
import com.certificateverification.model.Certificate;
import com.certificateverification.qr.*;
import com.certificateverification.repository.AuditLogRepository;
import com.certificateverification.service.CertificateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 5 Integration Tests for QR Code Generation and Verification.
 * Tests:
 * 1. Automatic QR generation upon certificate issuance
 * 2. ZXing encode and decode roundtrip
 * 3. Privacy compliance (non-sensitive QR payload)
 * 4. Verification cases: Genuine, Tampered, Not Found, Revoked, Invalid QR
 * 5. Manual Certificate ID entry
 * 6. QR REST APIs (/api/qr/generate/**, /api/qr/**, /api/qr/verify)
 * 7. Web UI (/verify-qr)
 * 8. Audit logging
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:test_qr_verification.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class QRVerificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private QRService qrService;

    @Autowired
    private QRCodeGenerator qrCodeGenerator;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Certificate issueTestCertificate(String certId, String studentName) {
        Certificate cert = new Certificate();
        cert.setCertificateId(certId);
        cert.setStudentName(studentName);
        cert.setCourseName("Cybersecurity & Forensics");
        cert.setInstitutionName("National Tech University");
        cert.setCertificateType("Degree");
        cert.setIssueDate(LocalDate.of(2024, 7, 1));
        return certificateService.issueCertificate(cert);
    }

    @Test
    void testAutomaticQRGenerationUponIssuance() {
        String certId = "CERT-QR-AUTO-1";
        Certificate cert = issueTestCertificate(certId, "Alice Wonder");

        assertNotNull(cert.getQrCodeData(), "Certificate must have qrCodeData populated");
        assertTrue(cert.getQrCodeData().startsWith("data:image/png;base64,"), "Must be a valid PNG Base64 data URI");

        // Decode the generated QR code image using ZXing
        Optional<String> decoded = qrCodeGenerator.decodeQRCodeFromBase64(cert.getQrCodeData());
        assertTrue(decoded.isPresent(), "ZXing must successfully decode the generated QR code");

        QRVerificationPayload payload = QRVerificationPayload.parse(decoded.get());
        assertEquals(certId, payload.getCertId());
        assertEquals(cert.getBlockchainHash(), payload.getRef());
    }

    @Test
    void testZXingEncodingAndDecodingRoundtrip() {
        String testData = "{\"certId\":\"TEST-123\",\"ref\":\"abc123def456\"}";
        byte[] qrBytes = qrCodeGenerator.generateQRCodeImageBytes(testData, 250, 250);
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 100);

        Optional<String> decoded = qrCodeGenerator.decodeQRCode(qrBytes);
        assertTrue(decoded.isPresent());
        assertEquals(testData, decoded.get());
    }

    @Test
    void testQRPayloadDoesNotExposeSensitiveInformation() {
        String certId = "CERT-PRIVACY-001";
        Certificate cert = issueTestCertificate(certId, "Very Private Student Name");

        String qrDataUri = cert.getQrCodeData();
        String decodedText = qrCodeGenerator.decodeQRCodeFromBase64(qrDataUri).orElse("");

        assertFalse(decodedText.contains("Very Private Student Name"), "QR payload must not contain student name");
        assertFalse(decodedText.contains("Cybersecurity & Forensics"), "QR payload must not contain course name");
        assertTrue(decodedText.contains(certId), "QR payload should identify certificate ID");
        assertTrue(decodedText.contains(cert.getBlockchainHash()), "QR payload should contain verification reference");
    }

    @Test
    void testVerifyQRResultGenuine() {
        String certId = "CERT-QR-GENUINE";
        Certificate cert = issueTestCertificate(certId, "John Genuine");

        String payload = QRVerificationPayload.buildPayloadString(certId, cert.getBlockchainHash());
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(payload)
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.100");

        assertNotNull(response);
        assertEquals("Genuine", response.getResult());
        assertEquals("GENUINE", response.getStatus());
        assertTrue(response.isVerified());
        assertTrue(response.isBlockchainMatch());
        assertEquals(certId, response.getCertificateId());
        assertEquals("John Genuine", response.getStudent());
        assertEquals("National Tech University", response.getInstitution());
    }

    @Test
    void testVerifyQRResultTampered() {
        String certId = "CERT-QR-TAMPER";
        issueTestCertificate(certId, "Bob Tampered");

        // QR payload with modified/tampered reference hash
        String tamperedRef = "0000000000000000000000000000000000000000000000000000000000000000";
        String payload = QRVerificationPayload.buildPayloadString(certId, tamperedRef);
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(payload)
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.101");

        assertNotNull(response);
        assertEquals("Tampered", response.getResult());
        assertEquals("TAMPERED", response.getStatus());
        assertFalse(response.isVerified());
        assertFalse(response.isBlockchainMatch());
    }

    @Test
    void testVerifyQRResultNotFound() {
        String payload = QRVerificationPayload.buildPayloadString("CERT-DOES-NOT-EXIST-404", "fakehash123");
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(payload)
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.102");

        assertNotNull(response);
        assertEquals("Not Found", response.getResult());
        assertEquals("NOT_FOUND", response.getStatus());
        assertFalse(response.isVerified());
    }

    @Test
    void testVerifyQRResultRevoked() {
        String certId = "CERT-QR-REVOKED";
        Certificate cert = issueTestCertificate(certId, "Charlie Revoked");
        certificateService.revokeCertificate(certId);

        String payload = QRVerificationPayload.buildPayloadString(certId, cert.getBlockchainHash());
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(payload)
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.103");

        assertNotNull(response);
        assertEquals("Revoked", response.getResult());
        assertEquals("REVOKED", response.getStatus());
        assertFalse(response.isVerified());
    }

    @Test
    void testVerifyQRInvalidQRCode() {
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue("This is some completely random unparseable string that exceeds standard bounds and has \n illegal characters")
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.104");

        assertNotNull(response);
        assertEquals("Invalid QR Code", response.getResult());
        assertEquals("INVALID_QR", response.getStatus());
        assertFalse(response.isVerified());
    }

    @Test
    void testVerifyManualCertificateId() {
        String certId = "CERT-QR-MANUAL";
        issueTestCertificate(certId, "Diana Prince");

        QRVerificationRequest request = QRVerificationRequest.builder()
                .certificateId(certId)
                .build();

        QRVerificationResponse response = qrService.verifyQRCode(request, "192.168.1.105");

        assertNotNull(response);
        assertEquals("Genuine", response.getResult());
        assertEquals("GENUINE", response.getStatus());
        assertTrue(response.isVerified());
        assertEquals("Diana Prince", response.getStudent());
    }

    @Test
    void testQRRestApiEndpoints() throws Exception {
        String certId = "CERT-QR-API-1";
        Certificate cert = issueTestCertificate(certId, "Edward Cullen");

        // 1. GET /api/qr/{id}
        mockMvc.perform(get("/api/qr/" + certId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateId").value(certId))
                .andExpect(jsonPath("$.qrCodeData").isNotEmpty())
                .andExpect(jsonPath("$.qrPayload").isNotEmpty());

        // 2. POST /api/qr/generate/{id}
        mockMvc.perform(post("/api/qr/generate/" + certId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateId").value(certId))
                .andExpect(jsonPath("$.message").value("QR code generated successfully."));

        // 3. POST /api/qr/verify
        String payload = QRVerificationPayload.buildPayloadString(certId, cert.getBlockchainHash());
        QRVerificationRequest req = QRVerificationRequest.builder().qrValue(payload).build();

        mockMvc.perform(post("/api/qr/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Genuine"))
                .andExpect(jsonPath("$.status").value("GENUINE"))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void testAuditLoggingForQRVerification() {
        String certId = "CERT-QR-AUDIT";
        issueTestCertificate(certId, "Audit User");

        QRVerificationRequest req = QRVerificationRequest.builder()
                .certificateId(certId)
                .build();

        qrService.verifyQRCode(req, "172.16.1.99");

        List<AuditLog> logs = auditLogRepository.findByCertificateIdOrderByTimestampDesc(certId);
        assertFalse(logs.isEmpty());
        AuditLog log = logs.get(0);
        assertEquals("VERIFIED_QR", log.getAction());
        assertEquals("172.16.1.99", log.getIpAddress());
        assertTrue(log.getDetails().contains("Result: Genuine"));
    }

    @Test
    void testWebQRVerifyEndpoints() throws Exception {
        // GET /verify-qr
        mockMvc.perform(get("/verify-qr"))
                .andExpect(status().isOk())
                .andExpect(view().name("qr-verify"))
                .andExpect(model().attributeExists("qrRequest"));

        // POST /verify-qr
        String certId = "CERT-QR-WEB-1";
        issueTestCertificate(certId, "Web User");

        mockMvc.perform(post("/verify-qr")
                        .param("certificateId", certId))
                .andExpect(status().isOk())
                .andExpect(view().name("qr-verify"))
                .andExpect(model().attributeExists("qrResponse"))
                .andExpect(model().attribute("verificationPerformed", true));
    }
}
