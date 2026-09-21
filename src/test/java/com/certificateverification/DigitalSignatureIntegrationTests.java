package com.certificateverification;

import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.Certificate;
import com.certificateverification.service.CertificateService;
import com.certificateverification.signature.DigitalSignatureService;
import com.certificateverification.signature.InstitutionKeyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 6 - Digital Signature Integration Tests.
 *
 * <p>Validates:
 * <ul>
 *   <li>RSA key pair generation</li>
 *   <li>Sign and verify round-trip</li>
 *   <li>Signature verification failure on tampered data</li>
 *   <li>Public key fingerprint generation</li>
 *   <li>InstitutionKeyStore seeding and lazy key creation</li>
 *   <li>Certificate issuance creates a valid digital signature</li>
 *   <li>Full 3-factor verification (blockchain + signature + not-revoked)</li>
 *   <li>GENUINE requires all three checks to pass</li>
 * </ul>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DigitalSignatureIntegrationTests {

    @Autowired
    private DigitalSignatureService digitalSignatureService;

    @Autowired
    private InstitutionKeyStore institutionKeyStore;

    @Autowired
    private CertificateService certificateService;

    private Certificate testCertificate;

    @BeforeEach
    void setUp() {
        testCertificate = new Certificate();
        testCertificate.setCertificateId("SIG-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        testCertificate.setStudentName("Alice Digital");
        testCertificate.setInstitutionName("State University");
        testCertificate.setCourseName("B.Tech Computer Science");
        testCertificate.setCertificateType("Degree");
        testCertificate.setIssueDate(LocalDate.of(2024, 6, 15));
        testCertificate.setStatus("ISSUED");
    }

    // =========================================================================
    // DigitalSignatureService unit-level tests
    // =========================================================================

    @Test
    @DisplayName("DS-01: RSA key pair generation returns non-null keys")
    void testKeyPairGeneration() {
        KeyPair keyPair = digitalSignatureService.generateKeyPair();
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("DS-02: Sign and verify round-trip succeeds for same data")
    void testSignAndVerifyRoundTrip() throws Exception {
        KeyPair keyPair = digitalSignatureService.generateKeyPair();
        String data = "CERT-TEST-001|Alice Digital|State University";

        String signature = digitalSignatureService.sign(data, keyPair.getPrivate());
        assertThat(signature).isNotNull().isNotEmpty();

        boolean valid = digitalSignatureService.verify(data, signature, keyPair.getPublic());
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("DS-03: Signature verification fails when data is tampered")
    void testSignatureFailsOnTamperedData() throws Exception {
        KeyPair keyPair = digitalSignatureService.generateKeyPair();
        String originalData = "CERT-TEST-001|Alice Digital|State University";
        String tamperedData  = "CERT-TEST-001|Bob Hacker|Fake University";

        String signature = digitalSignatureService.sign(originalData, keyPair.getPrivate());
        boolean valid = digitalSignatureService.verify(tamperedData, signature, keyPair.getPublic());
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("DS-04: Signature verification fails with wrong public key")
    void testSignatureFailsWithWrongPublicKey() throws Exception {
        KeyPair keyPairA = digitalSignatureService.generateKeyPair();
        KeyPair keyPairB = digitalSignatureService.generateKeyPair();
        String data = "some-certificate-hash";

        String signature = digitalSignatureService.sign(data, keyPairA.getPrivate());
        boolean valid = digitalSignatureService.verify(data, signature, keyPairB.getPublic());
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("DS-05: Public key fingerprint is non-empty and formatted with colons")
    void testPublicKeyFingerprint() {
        KeyPair keyPair = digitalSignatureService.generateKeyPair();
        String fingerprint = digitalSignatureService.getPublicKeyFingerprint(keyPair.getPublic());
        assertThat(fingerprint).isNotNull().isNotEmpty();
        assertThat(fingerprint).contains(":");
        // Format: XX:XX:... — each segment is 2 hex chars
        String[] parts = fingerprint.split(":");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(1);
        for (String part : parts) {
            assertThat(part).matches("[0-9A-F]{2}");
        }
    }

    @Test
    @DisplayName("DS-06: Public key encode/decode round-trip produces equivalent key")
    void testPublicKeyEncodeDecodeRoundTrip() throws Exception {
        KeyPair keyPair = digitalSignatureService.generateKeyPair();
        PublicKey original = keyPair.getPublic();

        String encoded = digitalSignatureService.encodePublicKey(original);
        assertThat(encoded).isNotNull().isNotEmpty();

        PublicKey decoded = digitalSignatureService.decodePublicKey(encoded);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getEncoded()).isEqualTo(original.getEncoded());
    }

    @Test
    @DisplayName("DS-07: verify() with null arguments returns false (no exception)")
    void testVerifyNullArguments() {
        boolean result = digitalSignatureService.verify(null, null, null);
        assertThat(result).isFalse();
    }

    // =========================================================================
    // InstitutionKeyStore tests
    // =========================================================================

    @Test
    @DisplayName("KS-01: Key store is seeded with default institutions at startup")
    void testKeyStoreSeedingOnStartup() {
        assertThat(institutionKeyStore.size()).isGreaterThanOrEqualTo(InstitutionKeyStore.DEFAULT_INSTITUTIONS.size());
        for (String institution : InstitutionKeyStore.DEFAULT_INSTITUTIONS) {
            assertThat(institutionKeyStore.hasKeyPair(institution))
                    .as("Expected key pair for institution: " + institution)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("KS-02: getOrCreateKeyPair creates key for unknown institution")
    void testLazyKeyCreation() {
        String newInstitution = "Unique Test University " + UUID.randomUUID();
        assertThat(institutionKeyStore.hasKeyPair(newInstitution)).isFalse();

        KeyPair keyPair = institutionKeyStore.getOrCreateKeyPair(newInstitution);
        assertThat(keyPair).isNotNull();
        assertThat(institutionKeyStore.hasKeyPair(newInstitution)).isTrue();
    }

    @Test
    @DisplayName("KS-03: getPublicKey returns null for unknown institution")
    void testGetPublicKeyUnknown() {
        PublicKey pk = institutionKeyStore.getPublicKey("Nonexistent University XYZ-" + UUID.randomUUID());
        assertThat(pk).isNull();
    }

    @Test
    @DisplayName("KS-04: getPublicKey returns non-null for known institution")
    void testGetPublicKeyKnown() {
        PublicKey pk = institutionKeyStore.getPublicKey("State University");
        assertThat(pk).isNotNull();
        assertThat(pk.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("KS-05: getOrCreateKeyPair returns same key pair on repeated calls")
    void testKeyPairIdempotency() {
        String institution = "Consistent University";
        KeyPair first  = institutionKeyStore.getOrCreateKeyPair(institution);
        KeyPair second = institutionKeyStore.getOrCreateKeyPair(institution);
        assertThat(first.getPublic().getEncoded())
                .isEqualTo(second.getPublic().getEncoded());
    }

    // =========================================================================
    // CertificateService issuance + signature tests
    // =========================================================================

    @Test
    @DisplayName("SVC-01: Issued certificate has non-null digital signature")
    void testIssuedCertificateHasDigitalSignature() {
        Certificate issued = certificateService.issueCertificate(testCertificate);
        assertThat(issued.getDigitalSignature()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("SVC-02: Issued certificate's signature is cryptographically valid")
    void testIssuedCertificateSignatureIsValid() {
        Certificate issued = certificateService.issueCertificate(testCertificate);

        String storedSignature = issued.getDigitalSignature();
        String blockchainHash  = issued.getBlockchainHash();
        PublicKey publicKey    = institutionKeyStore.getPublicKey(issued.getInstitutionName());

        assertThat(publicKey).isNotNull();
        boolean valid = digitalSignatureService.verify(blockchainHash, storedSignature, publicKey);
        assertThat(valid).isTrue();
    }

    // =========================================================================
    // Full 3-factor verification tests
    // =========================================================================

    @Test
    @DisplayName("VRF-01: GENUINE result when hash matches + signature valid + not revoked")
    void testGenuineWhenAllChecksPassed() {
        Certificate issued = certificateService.issueCertificate(testCertificate);

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(issued.getCertificateId())
                .studentName(issued.getStudentName())
                .courseName(issued.getCourseName())
                .institutionName(issued.getInstitutionName())
                .issueDate(issued.getIssueDate().toString())
                .certificateType(issued.getCertificateType())
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);

        assertThat(response.isVerified()).isTrue();
        assertThat(response.getResult()).isEqualTo("GENUINE CERTIFICATE");
        assertThat(response.isBlockchainMatch()).isTrue();
        assertThat(response.isSignatureValid()).isTrue();
        assertThat(response.getSignatureStatus()).isEqualTo("VALID");
        assertThat(response.getSigningInstitution()).isEqualTo(issued.getInstitutionName());
        assertThat(response.getPublicKeyFingerprint()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("VRF-02: TAMPERED result when hash does not match (wrong student name)")
    void testTamperedWhenHashMismatch() {
        Certificate issued = certificateService.issueCertificate(testCertificate);

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(issued.getCertificateId())
                .studentName("Tampered Hacker Name")    // <-- wrong name
                .courseName(issued.getCourseName())
                .institutionName(issued.getInstitutionName())
                .issueDate(issued.getIssueDate().toString())
                .certificateType(issued.getCertificateType())
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getResult()).isEqualTo("TAMPERED CERTIFICATE");
        assertThat(response.isBlockchainMatch()).isFalse();
    }

    @Test
    @DisplayName("VRF-03: REVOKED result when certificate is revoked")
    void testRevokedCertificate() {
        Certificate issued = certificateService.issueCertificate(testCertificate);
        certificateService.revokeCertificate(issued.getCertificateId());

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(issued.getCertificateId())
                .studentName(issued.getStudentName())
                .courseName(issued.getCourseName())
                .institutionName(issued.getInstitutionName())
                .issueDate(issued.getIssueDate().toString())
                .certificateType(issued.getCertificateType())
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getResult()).isEqualTo("REVOKED CERTIFICATE");
        assertThat(response.getCertificateStatus()).isEqualTo("REVOKED");
    }

    @Test
    @DisplayName("VRF-04: NOT FOUND result for non-existent certificate ID")
    void testNotFoundCertificate() {
        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId("NONEXISTENT-ID-XYZ-9999")
                .studentName("Ghost Student")
                .courseName("Ghost Course")
                .institutionName("Ghost University")
                .issueDate("2024-01-01")
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.getResult()).isEqualTo("Certificate Not Found / Potentially Fake");
    }

    @Test
    @DisplayName("VRF-05: signatureStatus is VALID for a properly signed issued certificate")
    void testSignatureStatusValidForIssuedCert() {
        Certificate issued = certificateService.issueCertificate(testCertificate);

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(issued.getCertificateId())
                .studentName(issued.getStudentName())
                .courseName(issued.getCourseName())
                .institutionName(issued.getInstitutionName())
                .issueDate(issued.getIssueDate().toString())
                .certificateType(issued.getCertificateType())
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);
        assertThat(response.getSignatureStatus()).isEqualTo("VALID");
    }

    @Test
    @DisplayName("VRF-06: Public key fingerprint is never null for signed certificates")
    void testPublicKeyFingerprintPresentForSignedCert() {
        Certificate issued = certificateService.issueCertificate(testCertificate);

        CertificateVerificationRequest req = CertificateVerificationRequest.builder()
                .certificateId(issued.getCertificateId())
                .studentName(issued.getStudentName())
                .courseName(issued.getCourseName())
                .institutionName(issued.getInstitutionName())
                .issueDate(issued.getIssueDate().toString())
                .certificateType(issued.getCertificateType())
                .build();

        CertificateVerificationResponse response = certificateService.verifyCertificate(req);
        assertThat(response.getPublicKeyFingerprint()).isNotNull().isNotEmpty();
    }
}
