package com.certificateverification.qr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generates and reads QR codes for certificates.
 * QR codes embed the certificate ID and blockchain hash, enabling
 * offline verification without an Internet connection.
 *
 * <p>Day 1: Placeholder - ZXing-based QR generation implemented in Day 2.</p>
 */
@Component
public class QRCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    /** Default QR code width in pixels. */
    public static final int QR_WIDTH = 300;

    /** Default QR code height in pixels. */
    public static final int QR_HEIGHT = 300;

    /**
     * Placeholder: Generate a QR code PNG for a certificate.
     * Full implementation in Day 2 using the ZXing library.
     *
     * @param certificateId   the unique certificate identifier
     * @param blockchainHash  the blockchain hash of the certificate
     * @return Base64-encoded PNG image of the QR code (placeholder)
     */
    public String generateQRCode(String certificateId, String blockchainHash) {
        logger.info("QRCodeGenerator.generateQRCode({}) - placeholder for Day 2", certificateId);
        return "placeholder-qr-code-base64";
    }

    /**
     * Placeholder: Decode QR code image data back to a certificate reference.
     * Full implementation in Day 2.
     *
     * @param qrCodeBase64 Base64-encoded QR code image
     * @return decoded certificate reference string
     */
    public String decodeQRCode(String qrCodeBase64) {
        logger.info("QRCodeGenerator.decodeQRCode() - placeholder for Day 2");
        return "placeholder-decoded-data";
    }
}
