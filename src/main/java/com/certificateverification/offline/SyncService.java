package com.certificateverification.offline;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.model.Block;
import com.certificateverification.model.Certificate;
import com.certificateverification.repository.BlockRepository;
import com.certificateverification.repository.CertificateRepository;
import com.certificateverification.signature.DigitalSignatureService;
import com.certificateverification.signature.InstitutionKeyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for creating, exporting, and importing offline synchronization packages.
 *
 * <p>Day 8: Bridges the Online/Institution ledger with the Offline Verifier by:
 * <ol>
 *   <li>Exporting blockchain blocks, certificate metadata, and public keys to JSON.</li>
 *   <li>Saving snapshots to {@code offline_sync/blockchain_snapshot.json}.</li>
 *   <li>Importing snapshots into the local offline cache for offline verification.</li>
 * </ol></p>
 */
@Service
public class SyncService {

    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

    public static final String DEFAULT_SYNC_DIR = "offline_sync";
    public static final String DEFAULT_SYNC_FILE = "offline_sync/blockchain_snapshot.json";

    private final Blockchain blockchain;
    private final BlockRepository blockRepository;
    private final CertificateRepository certificateRepository;
    private final InstitutionKeyStore institutionKeyStore;
    private final DigitalSignatureService digitalSignatureService;
    private final OfflineVerificationService offlineVerificationService;
    private final ObjectMapper objectMapper;

    private LocalDateTime lastSyncTime;
    private OfflineSyncPackage lastSyncPackage;

    @Autowired
    public SyncService(Blockchain blockchain,
                       BlockRepository blockRepository,
                       CertificateRepository certificateRepository,
                       InstitutionKeyStore institutionKeyStore,
                       DigitalSignatureService digitalSignatureService,
                       OfflineVerificationService offlineVerificationService,
                       ObjectMapper objectMapper) {
        this.blockchain = blockchain;
        this.blockRepository = blockRepository;
        this.certificateRepository = certificateRepository;
        this.institutionKeyStore = institutionKeyStore;
        this.digitalSignatureService = digitalSignatureService;
        this.offlineVerificationService = offlineVerificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * On startup, attempt to automatically load existing snapshot file if available.
     */
    @PostConstruct
    public void init() {
        File snapshotFile = new File(DEFAULT_SYNC_FILE);
        if (snapshotFile.exists() && snapshotFile.length() > 0) {
            try {
                logger.info("Found existing snapshot at {}. Loading into offline verifier...", DEFAULT_SYNC_FILE);
                importSyncPackage(snapshotFile);
            } catch (Exception e) {
                logger.warn("Could not load initial sync package: {}", e.getMessage());
            }
        } else {
            logger.info("No existing snapshot found at {}. Snapshot will be created on first sync generation.", DEFAULT_SYNC_FILE);
        }
    }

    /**
     * Generate an offline sync package from the current live database and keystore,
     * serialize it to JSON, write to {@code offline_sync/blockchain_snapshot.json},
     * and load it into the offline verifier cache.
     *
     * @return the generated sync package
     * @throws IOException if writing to the snapshot file fails
     */
    public synchronized OfflineSyncPackage generateSyncPackage() throws IOException {
        logger.info("Generating offline blockchain synchronization package...");

        // 1. Gather all blockchain blocks
        List<Block> liveBlocks = blockRepository.findAllByOrderByIndexAsc();
        if (liveBlocks.isEmpty()) {
            liveBlocks = blockchain.getChain();
        }

        List<OfflineBlock> offlineBlocks = new ArrayList<>();
        for (Block b : liveBlocks) {
            offlineBlocks.add(OfflineBlock.builder()
                    .index(b.getIndex())
                    .timestamp(b.getTimestamp())
                    .certificateId(b.getCertificateId())
                    .certificateHash(b.getCertificateHash())
                    .previousHash(b.getPreviousHash())
                    .hash(b.getHash())
                    .build());
        }

        // 2. Gather all certificates
        List<Certificate> liveCerts = certificateRepository.findAll();
        List<OfflineCertificateRecord> offlineCerts = new ArrayList<>();
        for (Certificate c : liveCerts) {
            offlineCerts.add(OfflineCertificateRecord.builder()
                    .certificateId(c.getCertificateId())
                    .studentName(c.getStudentName())
                    .courseName(c.getCourseName())
                    .institutionName(c.getInstitutionName())
                    .certificateType(c.getCertificateType())
                    .issueDate(c.getIssueDate() != null ? c.getIssueDate().toString() : "")
                    .expiryDate(c.getExpiryDate() != null ? c.getExpiryDate().toString() : "")
                    .status(c.getEffectiveStatus())
                    .blockchainHash(c.getBlockchainHash())
                    .digitalSignature(c.getDigitalSignature())
                    .revoked(c.isRevoked() || "REVOKED".equalsIgnoreCase(c.getStatus()))
                    .revocationReason(c.getRevocationReason())
                    .revocationTimestamp(c.getRevocationTimestamp() != null ? c.getRevocationTimestamp().toString() : "")
                    .build());
        }

        // 3. Gather public keys
        Map<String, String> publicKeys = new HashMap<>();
        Map<String, PublicKey> registeredKeys = institutionKeyStore.getAllPublicKeys();
        for (Map.Entry<String, PublicKey> entry : registeredKeys.entrySet()) {
            try {
                String base64Key = digitalSignatureService.encodePublicKey(entry.getValue());
                publicKeys.put(entry.getKey(), base64Key);
            } catch (Exception e) {
                logger.warn("Failed to encode public key for '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        int totalBlocks = offlineBlocks.size();
        int totalCertificates = offlineCerts.size();
        int totalRevokedCertificates = (int) offlineCerts.stream().filter(OfflineCertificateRecord::isRevoked).count();

        // 4. Assemble package
        OfflineSyncPackage syncPackage = OfflineSyncPackage.builder()
                .generatedAt(Instant.now().toString())
                .version("1.0")
                .blockchainBlocks(offlineBlocks)
                .certificates(offlineCerts)
                .publicKeys(publicKeys)
                .totalBlocks(totalBlocks)
                .totalCertificates(totalCertificates)
                .totalRevokedCertificates(totalRevokedCertificates)
                .build();

        // 5. Write to offline_sync/blockchain_snapshot.json
        Path syncDirPath = Paths.get(DEFAULT_SYNC_DIR);
        if (!Files.exists(syncDirPath)) {
            Files.createDirectories(syncDirPath);
        }

        File targetFile = new File(DEFAULT_SYNC_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, syncPackage);
        logger.info("Successfully wrote offline sync package to {} (size: {} bytes)",
                targetFile.getAbsolutePath(), targetFile.length());

        // 6. Load into offline cache
        offlineVerificationService.loadFromSyncPackage(syncPackage);
        this.lastSyncPackage = syncPackage;
        this.lastSyncTime = LocalDateTime.now();

        return syncPackage;
    }

    /**
     * Import a sync package from a specific file and load it into the offline verifier cache.
     *
     * @param file the snapshot JSON file
     * @return the loaded sync package
     * @throws IOException if reading or deserialization fails
     */
    public synchronized OfflineSyncPackage importSyncPackage(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Snapshot file does not exist: " + (file != null ? file.getPath() : "null"));
        }

        logger.info("Importing offline sync package from {}", file.getAbsolutePath());
        OfflineSyncPackage syncPackage = objectMapper.readValue(file, OfflineSyncPackage.class);
        offlineVerificationService.loadFromSyncPackage(syncPackage);

        this.lastSyncPackage = syncPackage;
        this.lastSyncTime = LocalDateTime.now();
        logger.info("Successfully imported sync package: {} blocks, {} certs, {} revoked",
                syncPackage.getTotalBlocks(), syncPackage.getTotalCertificates(), syncPackage.getTotalRevokedCertificates());

        return syncPackage;
    }

    /**
     * Import the default sync package from {@code offline_sync/blockchain_snapshot.json}.
     */
    public synchronized OfflineSyncPackage importDefaultSyncPackage() throws IOException {
        return importSyncPackage(new File(DEFAULT_SYNC_FILE));
    }

    /**
     * Last time a sync package was generated or imported.
     */
    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    /**
     * Most recently generated or imported package.
     */
    public OfflineSyncPackage getLastSyncPackage() {
        return lastSyncPackage;
    }

    /**
     * Check if the snapshot JSON file exists on disk.
     */
    public boolean isSyncFileExists() {
        return new File(DEFAULT_SYNC_FILE).exists();
    }

    /**
     * Return default snapshot file path.
     */
    public String getSyncFilePath() {
        return DEFAULT_SYNC_FILE;
    }

    /**
     * Return count of blocks in current snapshot.
     */
    public int getBlockCount() {
        return lastSyncPackage != null ? lastSyncPackage.getTotalBlocks() : 0;
    }

    /**
     * Return count of certificates in current snapshot.
     */
    public int getCertificateCount() {
        return lastSyncPackage != null ? lastSyncPackage.getTotalCertificates() : 0;
    }

    /**
     * Return count of revoked certificates in current snapshot.
     */
    public int getRevokedCount() {
        return lastSyncPackage != null ? lastSyncPackage.getTotalRevokedCertificates() : 0;
    }
}
