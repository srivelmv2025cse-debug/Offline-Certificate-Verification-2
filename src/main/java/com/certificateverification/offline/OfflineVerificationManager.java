package com.certificateverification.offline;

import com.certificateverification.qr.QRService;
import com.certificateverification.qr.QRVerificationRequest;
import com.certificateverification.qr.QRVerificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages the offline verification capability.
 * Allows certificate verification without an active Internet connection
 * by querying locally cached blockchain data and decoding QR codes.
 *
 * <p>Day 5: Integrated with QRService for offline QR code verification.</p>
 */
@Component
public class OfflineVerificationManager {

    private static final Logger logger = LoggerFactory.getLogger(OfflineVerificationManager.class);

    private final QRService qrService;

    @Autowired
    public OfflineVerificationManager(QRService qrService) {
        this.qrService = qrService;
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
     * Full offline verification returning complete response.
     */
    public QRVerificationResponse verifyOfflineDetails(String qrCodeData) {
        QRVerificationRequest request = QRVerificationRequest.builder()
                .qrValue(qrCodeData)
                .build();
        return qrService.verifyQRCode(request, "127.0.0.1-offline");
    }

    /**
     * Placeholder: Export the local blockchain snapshot for offline use.
     */
    public String exportBlockchainSnapshot() {
        logger.info("OfflineVerificationManager.exportBlockchainSnapshot()");
        return "{\"status\": \"snapshot-ready\"}";
    }

    /**
     * Placeholder: Import and sync a blockchain snapshot.
     */
    public void importBlockchainSnapshot(String snapshot) {
        logger.info("OfflineVerificationManager.importBlockchainSnapshot()");
    }
}
