package com.certificateverification.qr;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generates and decodes QR codes for certificates using the ZXing library.
 * QR codes encode certificate ID and cryptographic verification reference,
 * enabling offline verification without an Internet connection.
 *
 * <p>Day 5: Full ZXing QR code generation and decoding implementation.</p>
 */
@Component
public class QRCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    public static final int DEFAULT_WIDTH = 300;
    public static final int DEFAULT_HEIGHT = 300;

    /**
     * Generate a QR code PNG image as byte array.
     *
     * @param content data to encode
     * @param width   image width in pixels
     * @param height  image height in pixels
     * @return PNG image byte array
     */
    public byte[] generateQRCodeImageBytes(String content, int width, int height) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content to encode in QR code cannot be empty");
        }
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            logger.error("Failed to generate QR code image", e);
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code as Base64 Data URI string (e.g. data:image/png;base64,...).
     *
     * @param content data to encode
     * @param width   image width in pixels
     * @param height  image height in pixels
     * @return Base64 data URI string
     */
    public String generateQRCodeDataUri(String content, int width, int height) {
        byte[] imageBytes = generateQRCodeImageBytes(content, width, height);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }

    /**
     * Overload helper using default dimensions (300x300).
     */
    public String generateQRCodeDataUri(String content) {
        return generateQRCodeDataUri(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Backward-compatible helper method.
     *
     * @param certificateId  unique certificate identifier
     * @param blockchainHash blockchain hash of the certificate
     * @return Base64 data URI of QR code
     */
    public String generateQRCode(String certificateId, String blockchainHash) {
        String payload = QRVerificationPayload.buildPayloadString(certificateId, blockchainHash);
        return generateQRCodeDataUri(payload);
    }

    /**
     * Decode text from a QR code PNG image byte array.
     *
     * @param imageBytes PNG image bytes
     * @return Optional containing decoded text, or empty if decoding failed
     */
    public Optional<String> decodeQRCode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                return Optional.empty();
            }

            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return Optional.ofNullable(result.getText());
        } catch (NotFoundException e) {
            logger.warn("No QR code found in image");
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error decoding QR code", e);
            return Optional.empty();
        }
    }

    /**
     * Decode text from a Base64 data URI or plain Base64 string.
     *
     * @param base64Image Base64 string (with or without data:image/png;base64, prefix)
     * @return decoded string content
     */
    public Optional<String> decodeQRCodeFromBase64(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            return Optional.empty();
        }
        String cleanBase64 = base64Image.trim();
        if (cleanBase64.contains(",")) {
            cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(cleanBase64);
            return decodeQRCode(bytes);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Base64 string provided for QR decoding");
            return Optional.empty();
        }
    }
}
