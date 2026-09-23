package com.certificateverification.offline;

import com.certificateverification.qr.QRService;
import com.certificateverification.qr.QRVerificationRequest;
import com.certificateverification.qr.QRVerificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages the offline verification capability.
 * Allows certificate verification without an active Internet connection
 * by querying locally cached blockchain data and decoding QR codes.
 *
 * <p>Day 8: Fully integrated with {@link OfflineVerificationService} and {@link SyncService}.</p>
 */
@Component
public class OfflineVerificationManager {

    private static final Logger logger = LoggerFactory.getLogger(OfflineVerificationManager.class);

    private final QRService qrService;
    private final OfflineVerificationService offlineVerificationService;
    private final SyncService syncService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OfflineVerificationManager(QRService qrService,
                                     OfflineVerificationService offlineVerificationService,
                                     SyncService syncService,
                                     ObjectMapper objectMapper) {
        this.qrService = qrService;
        this.offlineVerificationService = offlineVerificationService;
        this.syncService = syncService;
        this.objectMapper = objectMapper;
    }

    /**
     * Verify a certificate offline using its QR code data or manual certificate ID.
     *
     * @param qrCodeData the data extracted from the certificate's QR code
     * @return true if the certificate is verified as genuine, false otherwise
     */
    public boolean verifyOffline(String qrCodeData) {
        logger.info("Verifying certificate offline using QR data");
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(qrCodeData)
                .build();
        QRVerificationResponse response = qrService.verifyQRCode(request, "127.0.0.1-offline");
        return response.isVerified();
    }

    /**
     * Full offline verification returning complete QR response.
     */
    public QRVerificationResponse verifyOfflineDetails(String qrCodeData) {
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(qrCodeData)
                .build();
        return qrService.verifyQRCode(request, "127.0.0.1-offline");
    }

    /**
     * Verify a certificate ID strictly offline using the local synchronized snapshot.
     */
    public OfflineVerificationResult verifyCertificateOffline(String certificateId) {
        return offlineVerificationService.verifyOffline(certificateId);
    }

    /**
     * Export the local blockchain snapshot for offline use as JSON string.
     */
    public String exportBlockchainSnapshot() {
        logger.info("OfflineVerificationManager: Generating offline snapshot");
        try {
            OfflineSyncPackage pkg = syncService.generateSyncPackage();
            return objectMapper.writeValueAsString(pkg);
        } catch (Exception e) {
            logger.error("Failed to export blockchain snapshot: {}", e.getMessage(), e);
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Import and sync a blockchain snapshot from a JSON string.
     */
    public void importBlockchainSnapshot(String snapshotJson) {
        logger.info("OfflineVerificationManager: Importing blockchain snapshot from JSON string");
        try {
            OfflineSyncPackage pkg = objectMapper.readValue(snapshotJson, OfflineSyncPackage.class);
            offlineVerificationService.loadFromSyncPackage(pkg);
        } catch (Exception e) {
            logger.error("Failed to import blockchain snapshot: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import snapshot", e);
        }
    }

    public OfflineVerificationService getOfflineVerificationService() {
        return offlineVerificationService;
    }

    public SyncService getSyncService() {
        return syncService;
    }
}
