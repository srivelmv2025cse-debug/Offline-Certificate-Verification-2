package com.certificateverification;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.model.Certificate;
import com.certificateverification.offline.*;
import com.certificateverification.repository.CertificateRepository;
import com.certificateverification.service.CertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 8 Integration Tests: Realistic Offline Certificate Verification & Blockchain Synchronization.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Offline sync package generation (blocks, certificates, RSA public keys)</li>
 *   <li>JSON snapshot file persistence at {@code offline_sync/blockchain_snapshot.json}</li>
 *   <li>Snapshot import and offline cache hydration</li>
 *   <li>Offline verification verdicts: GENUINE, TAMPERED, REVOKED, EXPIRED, NOT_FOUND</li>
 *   <li>Zero Internet / local data isolation validation</li>
 *   <li>Offline blockchain cryptographic chain integrity verification</li>
 *   <li>UI endpoints: /offline-verify, /sync-status, /sync/generate, /sync/import</li>
 * </ul></p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OfflineVerificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private Blockchain blockchain;

    @Autowired
    private SyncService syncService;

    @Autowired
    private OfflineBlockchainService offlineBlockchainService;

    @Autowired
    private OfflineVerificationService offlineVerificationService;

    @Autowired
    private OfflineVerificationManager offlineVerificationManager;

    @BeforeEach
    void setUp() {
        // Ensure Genesis block exists
        if (blockchain.getChain().isEmpty()) {
            blockchain.init();
        }
    }

    private Certificate createAndIssueCertificate(String studentName, String course, String inst, String type, LocalDate expiry) {
        Certificate cert = new Certificate();
        cert.setCertificateId("TEST-OFFLINE-" + System.nanoTime());
        cert.setStudentName(studentName);
        cert.setCourseName(course);
        cert.setInstitutionName(inst);
        cert.setCertificateType(type);
        LocalDate issueDate = (expiry != null && expiry.isBefore(LocalDate.now()))
                ? expiry.minusMonths(6)
                : LocalDate.now();
        cert.setIssueDate(issueDate);
        cert.setExpiryDate(expiry);
        cert.setStatus("ISSUED");
        return certificateService.issueCertificate(cert);
    }

    @Test
    @DisplayName("Generate sync package exports blocks, certificates, and RSA public keys to JSON file")
    void testGenerateSyncPackage() throws Exception {
        Certificate issued = createAndIssueCertificate("Alice Brown", "Cloud Computing", "State University", "Diploma", null);

        OfflineSyncPackage pkg = syncService.generateSyncPackage();

        assertNotNull(pkg, "Sync package must not be null");
        assertNotNull(pkg.getGeneratedAt(), "Snapshot must contain generation timestamp");
        assertEquals("1.0", pkg.getVersion());
        assertThat(pkg.getTotalBlocks()).isGreaterThanOrEqualTo(1);
        assertThat(pkg.getTotalCertificates()).isGreaterThanOrEqualTo(1);
        assertNotNull(pkg.getPublicKeys(), "Public keys map must not be null");
        assertThat(pkg.getPublicKeys()).isNotEmpty();

        // Verify JSON file exists on disk
        File snapshotFile = new File(SyncService.DEFAULT_SYNC_FILE);
        assertTrue(snapshotFile.exists(), "Snapshot file must exist at " + SyncService.DEFAULT_SYNC_FILE);
        assertThat(snapshotFile.length()).isGreaterThan(0);

        // Verify issued certificate is in the snapshot
        boolean found = pkg.getCertificates().stream()
                .anyMatch(c -> c.getCertificateId().equals(issued.getCertificateId()));
        assertTrue(found, "Issued certificate must be present in sync package");
    }

    @Test
    @DisplayName("Import sync package populates offline blockchain and certificate caches")
    void testImportSyncPackage() throws Exception {
        createAndIssueCertificate("Bob Wilson", "AI Foundations", "Anna University", "Degree", null);
        syncService.generateSyncPackage();

        OfflineSyncPackage imported = syncService.importDefaultSyncPackage();

        assertNotNull(imported);
        assertTrue(offlineBlockchainService.isLoaded(), "Offline blockchain must report loaded status");
        assertThat(offlineBlockchainService.getChainSize()).isGreaterThanOrEqualTo(1);
        assertThat(offlineVerificationService.getCertificateCacheSize()).isGreaterThanOrEqualTo(1);
        assertThat(offlineVerificationService.getPublicKeyCacheSize()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Offline verification verifies GENUINE certificate without Internet access")
    void testOfflineVerifyGenuine() throws Exception {
        Certificate cert = createAndIssueCertificate("Charlie Davis", "Cybersecurity", "State University", "Degree", null);
        syncService.generateSyncPackage();

        OfflineVerificationResult result = offlineVerificationService.verifyOffline(cert.getCertificateId());

        assertNotNull(result);
        assertEquals("GENUINE", result.getResult(), "Authentic certificate must verify as GENUINE");
        assertTrue(result.isBlockchainMatch(), "Blockchain hash must match");
        assertTrue(result.isSignatureValid(), "RSA digital signature must be valid");
        assertEquals("VALID", result.getSignatureStatus());
        assertEquals("Charlie Davis", result.getStudentName());
        assertEquals("Cybersecurity", result.getCourseName());
        assertEquals("State University", result.getInstitutionName());
    }

    @Test
    @DisplayName("Offline verification detects TAMPERED certificate when data fields are modified")
    void testOfflineVerifyTamperedFields() throws Exception {
        Certificate cert = createAndIssueCertificate("David Evans", "Data Science", "State University", "Degree", null);
        syncService.generateSyncPackage();

        // Attempt verification with modified student name (simulating altered paper credential)
        OfflineVerificationResult result = offlineVerificationService.verifyOffline(
                cert.getCertificateId(),
                "David Hacked Evans", // tampered student name
                null, null, null, null, null
        );

        assertNotNull(result);
        assertEquals("TAMPERED", result.getResult(), "Tampered data must yield TAMPERED verdict");
        assertFalse(result.isBlockchainMatch(), "Recalculated hash must not match blockchain");
        assertEquals("TAMPERED", result.getCertificateStatus());
    }

    @Test
    @DisplayName("Offline verification rejects REVOKED certificate — must never be reported as genuine")
    void testOfflineVerifyRevoked() throws Exception {
        Certificate cert = createAndIssueCertificate("Eve Taylor", "Bioinformatics", "State University", "Degree", null);
        certificateService.revokeCertificate(cert.getCertificateId(), "Academic dishonesty detected");
        syncService.generateSyncPackage();

        OfflineVerificationResult result = offlineVerificationService.verifyOffline(cert.getCertificateId());

        assertNotNull(result);
        assertEquals("REVOKED", result.getResult(), "Revoked certificate must be marked REVOKED offline");
        assertEquals("REVOKED", result.getCertificateStatus());
        assertThat(result.getMessage()).contains("Academic dishonesty detected");
        assertNotEquals("GENUINE", result.getResult(), "Revoked certificate must never be reported as genuine");
    }

    @Test
    @DisplayName("Offline verification flags EXPIRED certificate")
    void testOfflineVerifyExpired() throws Exception {
        Certificate cert = createAndIssueCertificate(
                "Frank Moore", "DevOps Engineering", "State University", "Certificate",
                LocalDate.now().minusDays(30) // Expired 30 days ago
        );
        syncService.generateSyncPackage();

        OfflineVerificationResult result = offlineVerificationService.verifyOffline(cert.getCertificateId());

        assertNotNull(result);
        assertEquals("EXPIRED", result.getResult(), "Expired certificate must be marked EXPIRED offline");
        assertEquals("EXPIRED", result.getCertificateStatus());
    }

    @Test
    @DisplayName("Offline verification returns NOT_FOUND for unknown certificate ID")
    void testOfflineVerifyNotFound() throws Exception {
        syncService.generateSyncPackage();

        OfflineVerificationResult result = offlineVerificationService.verifyOffline("UNKNOWN-CERT-" + System.nanoTime());

        assertNotNull(result);
        assertEquals("NOT_FOUND", result.getResult());
        assertThat(result.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Offline blockchain chain validation confirms cryptographic integrity of snapshot")
    void testOfflineBlockchainChainIntegrity() throws Exception {
        createAndIssueCertificate("Grace Hopper", "Computer Architecture", "State University", "Honorary", null);
        syncService.generateSyncPackage();

        boolean chainValid = offlineBlockchainService.isChainValid();
        assertTrue(chainValid, "Exported offline blockchain snapshot must be cryptographically valid");
    }

    @Test
    @DisplayName("OfflineVerificationManager delegates to OfflineVerificationService")
    void testOfflineVerificationManager() throws Exception {
        Certificate cert = createAndIssueCertificate("Heidi Clark", "Networks", "State University", "Diploma", null);
        syncService.generateSyncPackage();

        OfflineVerificationResult result = offlineVerificationManager.verifyCertificateOffline(cert.getCertificateId());
        assertNotNull(result);
        assertEquals("GENUINE", result.getResult());

        String snapshotJson = offlineVerificationManager.exportBlockchainSnapshot();
        assertNotNull(snapshotJson);
        assertThat(snapshotJson).contains("blockchainBlocks");
    }

    @Test
    @DisplayName("GET /offline-verify loads the offline verification page")
    void testGetOfflineVerifyPage() throws Exception {
        mockMvc.perform(get("/offline-verify"))
                .andExpect(status().isOk())
                .andExpect(view().name("offline-verify"))
                .andExpect(model().attributeExists("pageTitle"))
                .andExpect(model().attributeExists("isLoaded"));
    }

    @Test
    @DisplayName("POST /offline-verify performs verification and renders verdict")
    void testPostOfflineVerify() throws Exception {
        Certificate cert = createAndIssueCertificate("Ivan White", "Machine Learning", "State University", "Degree", null);
        syncService.generateSyncPackage();

        mockMvc.perform(post("/offline-verify")
                        .param("certificateId", cert.getCertificateId()))
                .andExpect(status().isOk())
                .andExpect(view().name("offline-verify"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("verificationPerformed", true));
    }

    @Test
    @DisplayName("GET /sync-status displays synchronization dashboard and stats")
    void testGetSyncStatusPage() throws Exception {
        syncService.generateSyncPackage();

        mockMvc.perform(get("/sync-status"))
                .andExpect(status().isOk())
                .andExpect(view().name("sync-status"))
                .andExpect(model().attributeExists("syncFilePath"))
                .andExpect(model().attributeExists("blockCount"))
                .andExpect(model().attributeExists("certificateCount"));
    }

    @Test
    @DisplayName("POST /sync/generate triggers package generation and redirects with flash message")
    void testPostSyncGenerate() throws Exception {
        mockMvc.perform(post("/sync/generate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sync-status"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /sync/import triggers package reload and redirects with flash message")
    void testPostSyncImport() throws Exception {
        syncService.generateSyncPackage();

        mockMvc.perform(post("/sync/import"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sync-status"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
