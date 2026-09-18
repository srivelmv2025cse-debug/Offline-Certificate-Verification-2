package com.certificateverification;

import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.AuditLog;
import com.certificateverification.model.Certificate;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 4 Integration Tests for Certificate Verification and Fraud/Tamper Detection.
 * Tests:
 * 1. Genuine certificate verification (ID found, hash matches -> "GENUINE CERTIFICATE")
 * 2. Modified / tampered certificate (ID found, hash mismatch -> "TAMPERED CERTIFICATE")
 * 3. Missing certificate (ID not found -> "Certificate Not Found / Potentially Fake")
 * 4. Revoked-status certificate (ID found, status REVOKED -> "REVOKED CERTIFICATE")
 * 5. REST API (POST /api/certificates/verify) with structured JSON
 * 6. Audit logging for every verification attempt
 * 7. Web UI endpoints (GET /verify, POST /verify)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:test_verify_blockchain.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CertificateVerificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Helper to issue a test certificate.
     */
    private Certificate issueTestCertificate(String certId, String studentName, String courseName, String institution) {
        Certificate cert = new Certificate();
        cert.setCertificateId(certId);
        cert.setStudentName(studentName);
        cert.setCourseName(courseName);
        cert.setInstitutionName(institution);
        cert.setCertificateType("Degree");
        cert.setIssueDate(LocalDate.of(2024, 6, 1));
        return certificateService.issueCertificate(cert);
    }

    @Test
    void testGenuineCertificateVerification() {
        String certId = "CERT-GENUINE-001";
        issueTestCertificate(certId, "Alice Johnson", "Computer Science", "Stanford University");

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Alice Johnson")
                .courseName("Computer Science")
                .institutionName("Stanford University")
                .issueDate("2024-06-01")
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(request, "192.168.1.50");

        assertNotNull(response);
        assertEquals("GENUINE CERTIFICATE", response.getResult());
        assertTrue(response.isBlockchainMatch(), "Blockchain hash and calculated hash must match");
        assertTrue(response.isVerified());
        assertEquals("ISSUED", response.getCertificateStatus());
        assertEquals(certId, response.getCertificateId());
        assertEquals("Alice Johnson", response.getStudent());
        assertEquals("Stanford University", response.getInstitution());
        assertEquals("Computer Science", response.getCourse());
        assertNotNull(response.getBlockchainHash());
        assertEquals(64, response.getBlockchainHash().length());
        assertEquals(response.getBlockchainHash(), response.getCalculatedHash());
    }

    @Test
    void testTamperedCertificateVerification() {
        String certId = "CERT-TAMPER-001";
        issueTestCertificate(certId, "Robert Smith", "Electrical Engineering", "MIT");

        // Request with altered student name (tampered credential)
        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Fake Robber") // Altered name
                .courseName("Electrical Engineering")
                .institutionName("MIT")
                .issueDate("2024-06-01")
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(request, "10.0.0.1");

        assertNotNull(response);
        assertEquals("TAMPERED CERTIFICATE", response.getResult());
        assertFalse(response.isBlockchainMatch(), "Blockchain hash and altered hash must not match");
        assertFalse(response.isVerified());
        assertNotNull(response.getBlockchainHash());
        assertNotNull(response.getCalculatedHash());
        assertNotEquals(response.getBlockchainHash(), response.getCalculatedHash());
    }

    @Test
    void testMissingCertificateVerification() {
        String nonExistentId = "CERT-MISSING-999";

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(nonExistentId)
                .studentName("Ghost Student")
                .courseName("Mystery Course")
                .institutionName("Unknown College")
                .issueDate("2024-01-01")
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(request, "10.0.0.2");

        assertNotNull(response);
        assertEquals("Certificate Not Found / Potentially Fake", response.getResult());
        assertFalse(response.isBlockchainMatch());
        assertFalse(response.isVerified());
        assertEquals("NOT_FOUND", response.getCertificateStatus());
        assertEquals("N/A", response.getBlockchainHash());
        assertNotNull(response.getCalculatedHash());
    }

    @Test
    void testRevokedCertificateVerification() {
        String certId = "CERT-REVOKED-001";
        issueTestCertificate(certId, "John Malicious", "Cybersecurity", "Oxford University");

        // Revoke the certificate
        Certificate revokedCert = certificateService.revokeCertificate(certId);
        assertEquals("REVOKED", revokedCert.getStatus());
        assertTrue(revokedCert.isRevoked());

        // Attempt verification with the genuine fields
        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("John Malicious")
                .courseName("Cybersecurity")
                .institutionName("Oxford University")
                .issueDate("2024-06-01")
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(request, "10.0.0.3");

        assertNotNull(response);
        assertEquals("REVOKED CERTIFICATE", response.getResult());
        assertEquals("REVOKED", response.getCertificateStatus());
        assertFalse(response.isVerified(), "Revoked certificate must not be marked verified");
    }

    @Test
    void testAuditLoggingForVerificationAttempts() {
        long initialAuditCount = auditLogRepository.count();

        String certId = "CERT-AUDIT-001";
        issueTestCertificate(certId, "Carol White", "Data Science", "Harvard");

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Carol White")
                .courseName("Data Science")
                .institutionName("Harvard")
                .issueDate("2024-06-01")
                .build();

        certificateService.verifyCertificate(request, "172.16.0.42");

        long updatedAuditCount = auditLogRepository.count();
        assertTrue(updatedAuditCount > initialAuditCount, "Audit log count must increase after verification");

        List<AuditLog> logs = auditLogRepository.findByCertificateIdOrderByTimestampDesc(certId);
        assertFalse(logs.isEmpty());
        AuditLog latestLog = logs.get(0);
        assertEquals(certId, latestLog.getCertificateId());
        assertEquals("VERIFIED", latestLog.getAction());
        assertEquals("172.16.0.42", latestLog.getIpAddress());
        assertTrue(latestLog.getDetails().contains("GENUINE CERTIFICATE"));
    }

    @Test
    void testPostVerifyApiGenuineCertificate() throws Exception {
        String certId = "CERT-API-GENUINE";
        issueTestCertificate(certId, "David Miller", "Mechanical Engineering", "Caltech");

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("David Miller")
                .courseName("Mechanical Engineering")
                .institutionName("Caltech")
                .issueDate("2024-06-01")
                .build();

        mockMvc.perform(post("/api/certificates/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("GENUINE CERTIFICATE"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.blockchainMatch").value(true))
                .andExpect(jsonPath("$.certificateId").value(certId))
                .andExpect(jsonPath("$.student").value("David Miller"))
                .andExpect(jsonPath("$.institution").value("Caltech"))
                .andExpect(jsonPath("$.course").value("Mechanical Engineering"))
                .andExpect(jsonPath("$.blockchainHash").isNotEmpty())
                .andExpect(jsonPath("$.calculatedHash").isNotEmpty())
                .andExpect(jsonPath("$.certificateStatus").value("ISSUED"));
    }

    @Test
    void testPostVerifyApiTamperedCertificate() throws Exception {
        String certId = "CERT-API-TAMPER";
        issueTestCertificate(certId, "Eva Green", "Bioengineering", "Cambridge");

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Eva Green Tampered")
                .courseName("Bioengineering")
                .institutionName("Cambridge")
                .issueDate("2024-06-01")
                .build();

        mockMvc.perform(post("/api/certificates/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("TAMPERED CERTIFICATE"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.blockchainMatch").value(false))
                .andExpect(jsonPath("$.certificateStatus").value("TAMPERED"));
    }

    @Test
    void testPostVerifyApiMissingCertificate() throws Exception {
        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId("CERT-NONEXISTENT")
                .studentName("Nobody")
                .courseName("None")
                .institutionName("Nowhere")
                .issueDate("2024-01-01")
                .build();

        mockMvc.perform(post("/api/certificates/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Certificate Not Found / Potentially Fake"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.blockchainMatch").value(false))
                .andExpect(jsonPath("$.certificateStatus").value("NOT_FOUND"));
    }

    @Test
    void testPostVerifyApiRevokedCertificate() throws Exception {
        String certId = "CERT-API-REVOKE";
        issueTestCertificate(certId, "Frank Hacker", "Network Admin", "NYU");
        certificateService.revokeCertificate(certId);

        CertificateVerificationRequest request = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Frank Hacker")
                .courseName("Network Admin")
                .institutionName("NYU")
                .issueDate("2024-06-01")
                .build();

        mockMvc.perform(post("/api/certificates/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REVOKED CERTIFICATE"))
                .andExpect(jsonPath("$.certificateStatus").value("REVOKED"))
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void testPostVerifyApiValidationErrors() throws Exception {
        // Missing certificateId
        CertificateVerificationRequest badRequest = CertificateVerificationRequest.builder()
                .studentName("Student")
                .build();

        mockMvc.perform(post("/api/certificates/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void testWebVerifyControllerPages() throws Exception {
        // GET /verify page
        mockMvc.perform(get("/verify"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attributeExists("verificationRequest"));

        // POST /verify form submission
        String certId = "CERT-WEB-001";
        issueTestCertificate(certId, "George Clark", "Economics", "LSE");

        mockMvc.perform(post("/verify")
                        .param("certificateId", certId)
                        .param("studentName", "George Clark")
                        .param("courseName", "Economics")
                        .param("institutionName", "LSE")
                        .param("issueDate", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("verify"))
                .andExpect(model().attributeExists("response"))
                .andExpect(model().attribute("verificationPerformed", true));
    }
}
