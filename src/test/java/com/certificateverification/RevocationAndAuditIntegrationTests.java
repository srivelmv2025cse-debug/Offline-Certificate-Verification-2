package com.certificateverification;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.AuditLog;
import com.certificateverification.model.Certificate;
import com.certificateverification.repository.AuditLogRepository;
import com.certificateverification.repository.CertificateRepository;
import com.certificateverification.service.AuditLogService;
import com.certificateverification.service.CertificateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 7 Integration Tests: Certificate Revocation and Audit Trail.
 * Tests cover revocation workflows, reason/timestamp recording, status lifecycle (VALID, REVOKED, EXPIRED),
 * verification rejections, comprehensive audit logging across operations, and REST APIs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RevocationAndAuditIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private Blockchain blockchain;

    @Autowired
    private ObjectMapper objectMapper;

    private Certificate createTestCert(String certId, String student, String course, String institution, LocalDate expiryDate) {
        certificateRepository.findByCertificateId(certId).ifPresent(c -> certificateRepository.delete(c));
        Certificate cert = new Certificate();
        cert.setCertificateId(certId);
        cert.setStudentName(student);
        cert.setCourseName(course);
        cert.setInstitutionName(institution);
        cert.setCertificateType("Degree");
        cert.setIssueDate(LocalDate.of(2024, 1, 15));
        cert.setExpiryDate(expiryDate);
        cert.setStatus("VALID");
        return certificateService.issueCertificate(cert);
    }

    // =========================================================================
    // 1. REVOCATION TESTS: Reason & Timestamp
    // =========================================================================

    @Test
    @DisplayName("REV-01: Revoking certificate records reason and timestamp correctly")
    void testRevokeCertificateWithReasonAndTimestamp() {
        String certId = "CERT-DAY7-REV-01";
        createTestCert(certId, "Alice Walker", "Cybersecurity", "MIT", null);

        LocalDateTime beforeRevoke = LocalDateTime.now().minusSeconds(1);
        String reason = "Honor code violation and academic misconduct";

        Certificate revoked = certificateService.revokeCertificate(certId, reason);

        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.isRevoked()).isTrue();
        assertThat(revoked.getRevocationReason()).isEqualTo(reason);
        assertThat(revoked.getRevocationTimestamp()).isNotNull();
        assertThat(revoked.getRevocationTimestamp()).isAfterOrEqualTo(beforeRevoke);
        assertThat(revoked.getEffectiveStatus()).isEqualTo("REVOKED");

        // Verify in DB directly
        Optional<Certificate> dbCert = certificateRepository.findByCertificateId(certId);
        assertThat(dbCert).isPresent();
        assertThat(dbCert.get().isRevoked()).isTrue();
        assertThat(dbCert.get().getRevocationReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("REV-02: Revoking with default reason works when none is supplied")
    void testRevokeCertificateDefaultReason() {
        String certId = "CERT-DAY7-REV-02";
        createTestCert(certId, "Bob Turner", "Robotics", "Carnegie Mellon", null);

        Certificate revoked = certificateService.revokeCertificate(certId);

        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.isRevoked()).isTrue();
        assertThat(revoked.getRevocationReason()).isEqualTo("Revoked by issuing authority");
        assertThat(revoked.getRevocationTimestamp()).isNotNull();
    }

    // =========================================================================
    // 2. STATUS LIFECYCLE: VALID, REVOKED, EXPIRED
    // =========================================================================

    @Test
    @DisplayName("STS-01: Status lifecycle distinguishes VALID, REVOKED, and EXPIRED")
    void testCertificateStatuses() {
        // Valid certificate
        String certValidId = "CERT-STS-VALID";
        Certificate validCert = createTestCert(certValidId, "Charlie Brown", "AI", "Oxford", LocalDate.now().plusYears(2));
        assertThat(validCert.getEffectiveStatus()).isEqualTo("VALID");
        assertThat(validCert.isExpired()).isFalse();

        // Expired certificate
        String certExpiredId = "CERT-STS-EXPIRED";
        Certificate expiredCert = createTestCert(certExpiredId, "Diana Prince", "Security", "Cambridge", LocalDate.now().minusDays(10));
        assertThat(expiredCert.isExpired()).isTrue();
        assertThat(expiredCert.getEffectiveStatus()).isEqualTo("EXPIRED");

        // Revoked certificate
        String certRevokedId = "CERT-STS-REVOKED";
        createTestCert(certRevokedId, "Edward Norton", "Art", "Yale", null);
        Certificate revokedCert = certificateService.revokeCertificate(certRevokedId, "Administrative annulment");
        assertThat(revokedCert.getEffectiveStatus()).isEqualTo("REVOKED");
    }

    @Test
    @DisplayName("STS-02: A revoked certificate must NEVER be reported as genuine during verification")
    void testRevokedCertificateNeverReportedAsGenuine() {
        String certId = "CERT-DAY7-NEVER-GENUINE";
        createTestCert(certId, "Frank Castle", "Law", "Columbia University", null);

        // Before revocation: verifies as genuine
        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Frank Castle")
                .courseName("Law")
                .institutionName("Columbia University")
                .issueDate("2024-01-15")
                .build();

        CertificateVerificationResponse respBefore = certificateService.verifyCertificate(req);
        assertThat(respBefore.isVerified()).isTrue();
        assertThat(respBefore.getResult()).isEqualTo("GENUINE CERTIFICATE");

        // Revoke certificate
        certificateService.revokeCertificate(certId, "Falsified admissions credentials");

        // After revocation: must NOT be genuine
        CertificateVerificationResponse respAfter = certificateService.verifyCertificate(req);
        assertThat(respAfter.isVerified()).isFalse();
        assertThat(respAfter.getResult()).isEqualTo("REVOKED CERTIFICATE");
        assertThat(respAfter.getCertificateStatus()).isEqualTo("REVOKED");
        assertThat(respAfter.getMessage()).contains("Falsified admissions credentials");
    }

    @Test
    @DisplayName("STS-03: Expired certificate is reported as EXPIRED and not genuine")
    void testExpiredCertificateVerification() {
        String certId = "CERT-DAY7-EXPIRED-VERIF";
        createTestCert(certId, "Grace Hopper", "Computer Science", "Harvard", LocalDate.now().minusMonths(1));

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(certId)
                .studentName("Grace Hopper")
                .courseName("Computer Science")
                .institutionName("Harvard")
                .issueDate("2024-01-15")
                .expiryDate(LocalDate.now().minusMonths(1).toString())
                .build();

        CertificateVerificationResponse resp = certificateService.verifyCertificate(req);
        assertThat(resp.isVerified()).isFalse();
        assertThat(resp.getResult()).isEqualTo("EXPIRED CERTIFICATE");
        assertThat(resp.getCertificateStatus()).isEqualTo("EXPIRED");
    }

    // =========================================================================
    // 3. AUDIT TRAIL LOGGING
    // =========================================================================

    @Test
    @DisplayName("AUD-01: Certificate issuance records an audit log entry")
    void testAuditLogForIssuance() {
        String certId = "CERT-DAY7-AUD-ISSUE";
        createTestCert(certId, "Henry Ford", "Engineering", "Purdue", null);

        List<AuditLog> logs = auditLogRepository.findByCertificateIdOrderByTimestampDesc(certId);
        assertThat(logs).isNotEmpty();

        AuditLog issuanceLog = logs.stream()
                .filter(l -> "CERTIFICATE_ISSUANCE".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(issuanceLog).isNotNull();
        assertThat(issuanceLog.getResult()).isEqualTo("SUCCESS");
        assertThat(issuanceLog.getDetails()).contains("Henry Ford");
        assertThat(issuanceLog.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("AUD-02: Certificate revocation records an audit log entry")
    void testAuditLogForRevocation() {
        String certId = "CERT-DAY7-AUD-REVOKE";
        createTestCert(certId, "Iris Murdoch", "Philosophy", "Oxford", null);

        certificateService.revokeCertificate(certId, "Senate revocation decree #42");

        List<AuditLog> logs = auditLogRepository.findByCertificateIdOrderByTimestampDesc(certId);
        AuditLog revLog = logs.stream()
                .filter(l -> "REVOCATION".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertThat(revLog).isNotNull();
        assertThat(revLog.getResult()).isEqualTo("SUCCESS");
        assertThat(revLog.getDetails()).contains("Senate revocation decree #42");
    }

    @Test
    @DisplayName("AUD-03: Blockchain validation records an audit log entry")
    void testAuditLogForBlockchainValidation() throws Exception {
        mockMvc.perform(get("/api/blockchain/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").isBoolean());

        List<AuditLog> chainLogs = auditLogRepository.findByCertificateIdOrderByTimestampDesc("BLOCKCHAIN_LEDGER");
        assertThat(chainLogs).isNotEmpty();
        AuditLog latestChainLog = chainLogs.get(0);
        assertThat(latestChainLog.getAction()).isEqualTo("BLOCKCHAIN_VALIDATION");
        assertThat(latestChainLog.getResult()).isIn("VALID", "TAMPERED");
    }

    // =========================================================================
    // 4. REST API: Revocation & Audit Logs
    // =========================================================================

    @Test
    @DisplayName("API-01: POST /api/certificates/{id}/revoke with JSON reason revokes successfully")
    void testRestApiRevocationWithJsonReason() throws Exception {
        String certId = "CERT-DAY7-API-REV";
        createTestCert(certId, "Jack London", "Literature", "Berkeley", null);

        Map<String, String> payload = Map.of("reason", "Revocation requested by registrar");

        mockMvc.perform(post("/api/certificates/" + certId + "/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certificate revoked successfully."))
                .andExpect(jsonPath("$.certificate.status").value("REVOKED"))
                .andExpect(jsonPath("$.certificate.revocationReason").value("Revocation requested by registrar"))
                .andExpect(jsonPath("$.certificate.revocationTimestamp").isNotEmpty());
    }

    @Test
    @DisplayName("API-02: GET /api/certificates/search returns matching certificates")
    void testRestApiSearchCertificates() throws Exception {
        String certId = "CERT-SEARCH-TEST-88";
        createTestCert(certId, "Searchable Student", "Quantum Computing", "Caltech", null);

        mockMvc.perform(get("/api/certificates/search").param("query", "Searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentName").value("Searchable Student"));
    }

    @Test
    @DisplayName("API-03: GET /api/audit-logs retrieves logs and supports filtering")
    void testRestApiAuditLogs() throws Exception {
        String certId = "CERT-API-AUD-LOGS";
        createTestCert(certId, "Karen Blixen", "History", "Copenhagen", null);

        // Retrieve all logs
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        // Retrieve filtered by certificateId
        mockMvc.perform(get("/api/audit-logs").param("certificateId", certId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].certificateId").value(certId));

        // Retrieve filtered by action
        mockMvc.perform(get("/api/audit-logs").param("action", "CERTIFICATE_ISSUANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("CERTIFICATE_ISSUANCE"));
    }

    // =========================================================================
    // 5. WEB UI PAGES: Revocation & Audit Trail
    // =========================================================================

    @Test
    @DisplayName("UI-01: GET /audit renders audit trail page with table")
    void testAuditTrailUiPage() throws Exception {
        mockMvc.perform(get("/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit"))
                .andExpect(model().attributeExists("auditLogs"))
                .andExpect(model().attributeExists("totalLogs"));
    }

    @Test
    @DisplayName("UI-02: GET /revoke renders revocation portal")
    void testRevokeUiPage() throws Exception {
        String certId = "CERT-UI-REV-PORTAL";
        createTestCert(certId, "Leo Tolstoy", "Literature", "Moscow State", null);

        // Search in revoke portal
        mockMvc.perform(get("/revoke").param("certificateId", certId))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attributeExists("foundCertificate"));
    }

    @Test
    @DisplayName("UI-03: POST /revoke handles revocation form and redirects")
    void testRevokeFormSubmission() throws Exception {
        String certId = "CERT-UI-REV-SUBMIT";
        createTestCert(certId, "Marie Curie", "Chemistry", "Sorbonne", null);

        mockMvc.perform(post("/revoke")
                        .param("certificateId", certId)
                        .param("reason", "Administrative recall"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/certificate/" + certId));

        Certificate cert = certificateService.getCertificateById(certId).orElseThrow();
        assertThat(cert.isRevoked()).isTrue();
        assertThat(cert.getRevocationReason()).isEqualTo("Administrative recall");
    }

    @Test
    @DisplayName("UI-04: GET /certificates supports search query param")
    void testCertificatesSearchUi() throws Exception {
        createTestCert("CERT-SEARCH-UI-01", "Nikola Tesla", "Electrical Engineering", "Graz", null);

        mockMvc.perform(get("/certificates").param("search", "Tesla"))
                .andExpect(status().isOk())
                .andExpect(view().name("certificates"))
                .andExpect(model().attributeExists("certificates"))
                .andExpect(model().attribute("searchQuery", "Tesla"));
    }
}
