package com.certificateverification.qr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-sensitive verification payload encoded in the QR code.
 *
 * <p>Identifies the certificate using only its Certificate ID and cryptographic
 * verification reference (SHA-256 blockchain hash). Conforms strictly to privacy
 * requirements by NEVER encoding sensitive student records or personal data.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QRVerificationPayload {

    private static final Logger logger = LoggerFactory.getLogger(QRVerificationPayload.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final String SYSTEM_TAG = "OFFLINE-CERT-V1";

    /** Unique certificate identifier */
    @JsonProperty("certId")
    private String certId;

    /** Cryptographic verification reference (blockchain hash) */
    @JsonProperty("ref")
    private String ref;

    /** System verification schema tag */
    @JsonProperty("system")
    @Builder.Default
    private String system = SYSTEM_TAG;

    /**
     * Build standard JSON string for QR encoding.
     */
    public static String buildPayloadString(String certificateId, String blockchainHash) {
        QRVerificationPayload payload = QRVerificationPayload.builder()
                .certId(certificateId != null ? certificateId.trim() : "")
                .ref(blockchainHash != null ? blockchainHash.trim() : "")
                .system(SYSTEM_TAG)
                .build();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing QR payload", e);
            // Fallback to formatted string if JSON serialization fails
            return "{\"certId\":\"" + certificateId + "\",\"ref\":\"" + (blockchainHash != null ? blockchainHash : "") + "\"}";
        }
    }

    /**
     * Parse raw QR code string, URL, or plain Certificate ID into a structured payload.
     *
     * @param rawText raw scanned QR text or manual input
     * @return parsed QRVerificationPayload
     * @throws IllegalArgumentException if the QR text is null, blank, or completely unparseable
     */
    public static QRVerificationPayload parse(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("QR code value is empty or invalid");
        }
        String clean = rawText.trim();

        // 1. Check if JSON payload
        if (clean.startsWith("{") && clean.endsWith("}")) {
            try {
                QRVerificationPayload parsed = objectMapper.readValue(clean, QRVerificationPayload.class);
                if (parsed.getCertId() != null && !parsed.getCertId().trim().isEmpty()) {
                    parsed.setCertId(parsed.getCertId().trim());
                    if (parsed.getRef() != null) {
                        parsed.setRef(parsed.getRef().trim());
                    }
                    return parsed;
                }
            } catch (Exception e) {
                logger.debug("Failed parsing JSON QR payload, checking alternative formats: {}", e.getMessage());
            }
        }

        // 2. Check if URL containing certificateId or certId parameter
        if (clean.contains("?") && (clean.contains("certificateId=") || clean.contains("certId=") || clean.contains("id="))) {
            String certId = null;
            String ref = null;
            String query = clean.substring(clean.indexOf("?") + 1);
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    String k = kv[0].trim();
                    String v = kv[1].trim();
                    if (k.equalsIgnoreCase("certificateId") || k.equalsIgnoreCase("certId") || k.equalsIgnoreCase("id")) {
                        certId = v;
                    } else if (k.equalsIgnoreCase("ref") || k.equalsIgnoreCase("hash")) {
                        ref = v;
                    }
                }
            }
            if (certId != null && !certId.isEmpty()) {
                return QRVerificationPayload.builder().certId(certId).ref(ref).system(SYSTEM_TAG).build();
            }
        }

        // 3. Check if custom URI format (e.g. CERT-VERIFY://CERT-ID/REF)
        if (clean.startsWith("CERT-VERIFY://")) {
            String path = clean.substring("CERT-VERIFY://".length());
            String[] parts = path.split("/");
            if (parts.length >= 1 && !parts[0].trim().isEmpty()) {
                String certId = parts[0].trim();
                String ref = parts.length > 1 ? parts[1].trim() : null;
                return QRVerificationPayload.builder().certId(certId).ref(ref).system(SYSTEM_TAG).build();
            }
        }

        // 4. Fallback: treat clean as direct Certificate ID if it has a reasonable certificate pattern
        // e.g., "CERT-...", alphanumeric with dashes/underscores, no linebreaks
        if (!clean.contains("\n") && clean.length() >= 3 && clean.length() <= 100) {
            return QRVerificationPayload.builder()
                    .certId(clean)
                    .ref(null)
                    .system(SYSTEM_TAG)
                    .build();
        }

        throw new IllegalArgumentException("Invalid QR code format: unable to recognize certificate identity");
    }
}
