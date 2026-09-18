package com.certificateverification.qr;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for QR verification requests.
 * Allows verification by entering a certificate ID manually,
 * providing the scanned QR text value, or both.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRVerificationRequest {

    /** Scanned QR code text or payload string */
    @JsonAlias({"qr", "qr_code", "payload", "scannedValue"})
    private String qrValue;

    /** Manually entered Certificate ID */
    @JsonAlias({"id", "certId", "certificate_id"})
    private String certificateId;

    /** Optional base64 encoded QR image for server-side decoding */
    private String qrImageBase64;
}
