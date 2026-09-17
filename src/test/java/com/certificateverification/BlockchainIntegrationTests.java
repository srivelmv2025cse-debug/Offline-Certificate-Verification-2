package com.certificateverification;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.blockchain.HashUtil;
import com.certificateverification.model.Block;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Day 3 Integration tests for Custom Java Blockchain:
 * - Block creation & fields
 * - SHA-256 hashing via Java MessageDigest
 * - Genesis block initialization
 * - Certificate block chaining
 * - Chain validation & tamper detection
 * - REST APIs (/api/blockchain, /api/blockchain/validate, /api/blockchain/add)
 * - Web controller (/blockchain)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:test_blockchain_verification.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BlockchainIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Blockchain blockchain;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSha256Hashing() {
        String input = "Offline Certificate Verification Blockchain";
        String hash1 = HashUtil.sha256(input);
        String hash2 = HashUtil.sha256(input);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2, "SHA-256 must be deterministic");

        // Hash of empty string
        String emptyHash = HashUtil.sha256("");
        assertEquals(64, emptyHash.length());
    }

    @Test
    void testCanonicalCertificateHashing() {
        String hash = HashUtil.hashCertificate(
                "CERT-B1", "Alice Smith", "Computer Science", "Stanford", "Degree", "2024-05-01", "2028-05-01"
        );
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void testGenesisBlockCreationAndStructure() {
        List<Block> chain = blockchain.getChain();
        assertFalse(chain.isEmpty(), "Chain should contain at least Genesis block");

        Block genesis = chain.get(0);
        assertEquals(0, genesis.getIndex(), "Genesis block must have index 0");
        assertEquals(Blockchain.GENESIS_PREVIOUS_HASH, genesis.getPreviousHash());
        assertNotNull(genesis.getHash());
        assertEquals(64, genesis.getHash().length());
        assertTrue(blockchain.isChainValid(), "Chain starting with Genesis must be valid");
    }

    @Test
    void testAddCertificateBlockAndLinkage() {
        int initialSize = blockchain.getChainSize();
        Block prevBlock = blockchain.getLatestBlock();

        String certId = "CERT-CHAIN-" + System.currentTimeMillis();
        String certHash = HashUtil.sha256("test-certificate-content");

        Block newBlock = blockchain.addCertificateBlock(certId, certHash);

        assertNotNull(newBlock);
        assertEquals(prevBlock.getIndex() + 1, newBlock.getIndex());
        assertEquals(prevBlock.getHash(), newBlock.getPreviousHash(), "New block must link to previous block's hash");
        assertEquals(certHash, newBlock.getCertificateHash());
        assertEquals(initialSize + 1, blockchain.getChainSize());
        assertTrue(blockchain.isChainValid(), "Chain must be valid after adding a block");
    }

    @Test
    void testCertificateIssuingAutomaticallyAnchorsToBlockchain() {
        Certificate cert = new Certificate();
        cert.setCertificateId("CERT-AUTO-ANCHOR-1");
        cert.setStudentName("Suresh Raina");
        cert.setCourseName("Information Security");
        cert.setInstitutionName("Cyber Institute");
        cert.setCertificateType("Certification");
        cert.setIssueDate(LocalDate.of(2024, 8, 15));

        Certificate saved = certificateService.issueCertificate(cert);

        assertNotNull(saved.getBlockchainHash(), "Certificate must have blockchainHash populated");
        assertEquals(64, saved.getBlockchainHash().length());

        // Verify block in blockchain matches
        Block latest = blockchain.getLatestBlock();
        assertEquals("CERT-AUTO-ANCHOR-1", latest.getCertificateId());
        assertEquals(saved.getBlockchainHash(), latest.getCertificateHash());
        assertTrue(blockchain.isChainValid());
    }

    @Test
    void testTamperDetectionOnBlockContent() {
        // Create a standalone blockchain for tamper testing
        com.certificateverification.repository.BlockRepository mockRepo = org.mockito.Mockito.mock(
                com.certificateverification.repository.BlockRepository.class
        );
        org.mockito.Mockito.when(mockRepo.findAllByOrderByIndexAsc()).thenReturn(new java.util.ArrayList<>());
        org.mockito.Mockito.when(mockRepo.save(org.mockito.ArgumentMatchers.any(Block.class)))
                .thenAnswer(i -> i.getArgument(0));

        Blockchain testChain = new Blockchain(mockRepo);
        testChain.init();

        Block b1 = testChain.addCertificateBlock("CERT-T1", HashUtil.sha256("cert1"));
        Block b2 = testChain.addCertificateBlock("CERT-T2", HashUtil.sha256("cert2"));

        assertTrue(testChain.isChainValid(), "Chain should be valid initially");

        // Tamper with Block 1 certificateHash
        b1.setCertificateHash(HashUtil.sha256("hacked-cert-data"));

        assertFalse(testChain.isChainValid(), "Blockchain must detect tampered block content!");
    }

    @Test
    void testTamperDetectionOnBrokenChainLink() {
        com.certificateverification.repository.BlockRepository mockRepo = org.mockito.Mockito.mock(
                com.certificateverification.repository.BlockRepository.class
        );
        org.mockito.Mockito.when(mockRepo.findAllByOrderByIndexAsc()).thenReturn(new java.util.ArrayList<>());
        org.mockito.Mockito.when(mockRepo.save(org.mockito.ArgumentMatchers.any(Block.class)))
                .thenAnswer(i -> i.getArgument(0));

        Blockchain testChain = new Blockchain(mockRepo);
        testChain.init();

        testChain.addCertificateBlock("CERT-LINK-1", HashUtil.sha256("link1"));
        Block b2 = testChain.addCertificateBlock("CERT-LINK-2", HashUtil.sha256("link2"));

        assertTrue(testChain.isChainValid());

        // Break previousHash linkage
        b2.setPreviousHash("fake_previous_hash_0000000000000000000000000000000000000000000000000");

        assertFalse(testChain.isChainValid(), "Blockchain must detect broken linkage!");
    }

    @Test
    void testRestApiEndpoints() throws Exception {
        // GET /api/blockchain
        mockMvc.perform(get("/api/blockchain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].index").value(0))
                .andExpect(jsonPath("$[0].certificateId").value(Blockchain.GENESIS_CERTIFICATE_ID));

        // GET /api/blockchain/validate
        mockMvc.perform(get("/api/blockchain/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("Blockchain Valid"))
                .andExpect(jsonPath("$.message").value("Blockchain Valid"));

        // POST /api/blockchain/add
        Map<String, String> payload = new HashMap<>();
        payload.put("certificateId", "CERT-REST-API-BLOCK");
        payload.put("certificateHash", HashUtil.sha256("rest-data"));

        mockMvc.perform(post("/api/blockchain/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Block added to blockchain successfully."))
                .andExpect(jsonPath("$.block.certificateId").value("CERT-REST-API-BLOCK"))
                .andExpect(jsonPath("$.block.hash").isNotEmpty());
    }

    @Test
    void testBlockchainWebPage() throws Exception {
        // GET /blockchain
        mockMvc.perform(get("/blockchain"))
                .andExpect(status().isOk())
                .andExpect(view().name("blockchain"))
                .andExpect(model().attributeExists("blocks"))
                .andExpect(model().attributeExists("totalBlocks"));

        // GET /blockchain?validate=true
        mockMvc.perform(get("/blockchain").param("validate", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("blockchain"))
                .andExpect(model().attribute("validationPerformed", true))
                .andExpect(model().attribute("isValid", true))
                .andExpect(model().attribute("validationMessage", "Blockchain Valid"));
    }
}
