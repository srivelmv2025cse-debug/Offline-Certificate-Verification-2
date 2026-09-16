package com.certificateverification;

import com.certificateverification.model.Certificate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 2 Integration and Controller tests for Certificate Issuing and Management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:test_certificate_verification.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CertificateManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testIssueCertificateSuccess() {
        Certificate cert = new Certificate();
        cert.setCertificateId("CERT-UNIT-001");
        cert.setStudentName("Jane Doe");
        cert.setCourseName("Data Science");
        cert.setInstitutionName("Tech University");
        cert.setCertificateType("Degree");
        cert.setIssueDate(LocalDate.of(2024, 5, 10));
        cert.setExpiryDate(LocalDate.of(2028, 5, 10));

        Certificate saved = certificateService.issueCertificate(cert);
        assertNotNull(saved.getId());
        assertEquals("CERT-UNIT-001", saved.getCertificateId());
        assertEquals("ISSUED", saved.getStatus());
        assertFalse(saved.isRevoked());
        assertNotNull(saved.getCreatedAt());

        Optional<Certificate> retrieved = certificateService.getCertificateById("CERT-UNIT-001");
        assertTrue(retrieved.isPresent());
        assertEquals("Jane Doe", retrieved.get().getStudentName());
    }

    @Test
    void testIssueCertificateValidationFailures() {
        // Missing Certificate ID
        Certificate c1 = new Certificate();
        c1.setStudentName("John");
        c1.setCourseName("CS");
        c1.setInstitutionName("Uni");
        c1.setCertificateType("Degree");
        c1.setIssueDate(LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> certificateService.issueCertificate(c1));

        // Duplicate Certificate ID
        Certificate c2 = new Certificate();
        c2.setCertificateId("CERT-DUP-01");
        c2.setStudentName("Alice");
        c2.setCourseName("Math");
        c2.setInstitutionName("Uni");
        c2.setCertificateType("Diploma");
        c2.setIssueDate(LocalDate.now());
        certificateService.issueCertificate(c2);

        Certificate c2Dup = new Certificate();
        c2Dup.setCertificateId("CERT-DUP-01");
        c2Dup.setStudentName("Bob");
        c2Dup.setCourseName("Physics");
        c2Dup.setInstitutionName("Uni");
        c2Dup.setCertificateType("Diploma");
        c2Dup.setIssueDate(LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> certificateService.issueCertificate(c2Dup));

        // Expiry date before issue date
        Certificate c3 = new Certificate();
        c3.setCertificateId("CERT-EXP-01");
        c3.setStudentName("Charlie");
        c3.setCourseName("Biology");
        c3.setInstitutionName("Uni");
        c3.setCertificateType("Degree");
        c3.setIssueDate(LocalDate.of(2025, 1, 1));
        c3.setExpiryDate(LocalDate.of(2024, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> certificateService.issueCertificate(c3));
    }

    @Test
    void testRevokeCertificate() {
        Certificate cert = new Certificate();
        cert.setCertificateId("CERT-REV-001");
        cert.setStudentName("Mark");
        cert.setCourseName("Engineering");
        cert.setInstitutionName("Tech Institute");
        cert.setCertificateType("Degree");
        cert.setIssueDate(LocalDate.now());

        certificateService.issueCertificate(cert);
        Certificate revoked = certificateService.revokeCertificate("CERT-REV-001");
        assertEquals("REVOKED", revoked.getStatus());
        assertTrue(revoked.isRevoked());
    }

    @Test
    void testGetAllCertificates() {
        List<Certificate> list = certificateService.getAllCertificates();
        assertNotNull(list);
    }

    @Test
    void testRestApiEndpoints() throws Exception {
        // Test health endpoint
        mockMvc.perform(get("/api/certificates/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Test issue certificate via REST API
        Certificate apiCert = new Certificate();
        apiCert.setCertificateId("CERT-REST-001");
        apiCert.setStudentName("Rest User");
        apiCert.setCourseName("Cloud Computing");
        apiCert.setInstitutionName("Cloud Academy");
        apiCert.setCertificateType("Certification");
        apiCert.setIssueDate(LocalDate.of(2024, 6, 1));

        mockMvc.perform(post("/api/certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiCert)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Certificate issued successfully."))
                .andExpect(jsonPath("$.certificate.certificateId").value("CERT-REST-001"));

        // Test get certificate by ID via REST
        mockMvc.perform(get("/api/certificates/CERT-REST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Rest User"));

        // Test get not found certificate
        mockMvc.perform(get("/api/certificates/NON-EXISTENT"))
                .andExpect(status().isNotFound());

        // Test get all certificates via REST
        mockMvc.perform(get("/api/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test revoke via REST
        mockMvc.perform(post("/api/certificates/CERT-REST-001/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certificate revoked successfully."))
                .andExpect(jsonPath("$.certificate.status").value("REVOKED"));
    }

    @Test
    void testWebControllerPages() throws Exception {
        // GET /issue
        mockMvc.perform(get("/issue"))
                .andExpect(status().isOk())
                .andExpect(view().name("issue"));

        // GET /certificates
        mockMvc.perform(get("/certificates"))
                .andExpect(status().isOk())
                .andExpect(view().name("certificates"));

        // POST /issue form submit
        mockMvc.perform(post("/issue")
                        .param("certificateId", "CERT-WEB-001")
                        .param("studentName", "Web Student")
                        .param("courseName", "Web Development")
                        .param("institutionName", "Web Academy")
                        .param("certificateType", "Diploma")
                        .param("issueDate", "2024-04-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/certificate/CERT-WEB-001"))
                .andExpect(flash().attribute("successMessage", "Certificate issued successfully."));

        // GET /certificate/CERT-WEB-001 details page
        mockMvc.perform(get("/certificate/CERT-WEB-001"))
                .andExpect(status().isOk())
                .andExpect(view().name("certificate-details"));
    }
}
