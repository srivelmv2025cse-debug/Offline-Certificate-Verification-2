package com.certificateverification.signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Day 6 - Digital Signature Service.
 *
 * <p>Provides RSA-2048 digital signature operations using Java's built-in
 * {@link java.security} APIs (no external cryptographic libraries required).
 *
 * <p><strong>Algorithm:</strong> SHA256withRSA (RSA-2048 key, SHA-256 digest)
 *
 * <p><strong>IMPORTANT — Educational Project Notice:</strong>
 * <ul>
 *   <li>Private keys are held in memory (InstitutionKeyStore) for this demonstration.</li>
 *   <li>In a production system, private keys MUST be stored in a Hardware Security Module (HSM),
 *       Java KeyStore (.jks / .p12 with strong passwords), or a dedicated secrets manager
 *       (e.g., HashiCorp Vault, AWS KMS). They must NEVER be stored in plain text or a database.</li>
 *   <li>Public keys may be distributed freely; only the public key fingerprint is shown in the UI.</li>
 * </ul>
 */
@Service
public class DigitalSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(DigitalSignatureService.class);

    /** RSA key algorithm */
    public static final String KEY_ALGORITHM = "RSA";

    /** Signature algorithm: RSA with SHA-256 digest */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** RSA key size in bits (2048-bit provides adequate security for this project) */
    public static final int KEY_SIZE = 2048;

    // -------------------------------------------------------------------------
    // KeyPair Generation
    // -------------------------------------------------------------------------

    /**
     * Generate a new RSA-2048 key pair.
     *
     * <p>Uses Java's {@link KeyPairGenerator} with the platform's default
     * {@link SecureRandom} source.
     *
     * @return freshly generated RSA {@link KeyPair}
     */
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            logger.info("RSA-{} key pair generated successfully", KEY_SIZE);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            // RSA is mandatory in every Java SE implementation (JCA requirement).
            throw new IllegalStateException("RSA algorithm not available in JVM — this should never happen", e);
        }
    }

    // -------------------------------------------------------------------------
    // Signing
    // -------------------------------------------------------------------------

    /**
     * Sign data using the supplied RSA private key.
     *
     * <p>The data string is signed as UTF-8 bytes using SHA256withRSA.
     * The signature is returned as a Base64-encoded string suitable for storage
     * in a text column.
     *
     * @param data       the plaintext data to sign (e.g., the certificate hash)
     * @param privateKey the institution's RSA private key
     * @return Base64-encoded RSA signature string
     * @throws SignatureException if signing fails
     */
    public String sign(String data, PrivateKey privateKey) throws SignatureException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data to sign must not be null or empty");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key must not be null");
        }
        try {
            Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
            signer.initSign(privateKey, new SecureRandom());
            signer.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] signatureBytes = signer.sign();
            String encoded = Base64.getEncoder().encodeToString(signatureBytes);
            logger.debug("Signed data; signature length={} bytes", signatureBytes.length);
            return encoded;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to initialise RSA signer", e);
        }
    }

    // -------------------------------------------------------------------------
    // Verification
    // -------------------------------------------------------------------------

    /**
     * Verify that a Base64-encoded RSA signature is valid for the given data
     * using the supplied public key.
     *
     * @param data            original data that was signed (e.g., the certificate hash)
     * @param base64Signature Base64-encoded signature returned by {@link #sign}
     * @param publicKey       institution's RSA public key
     * @return {@code true} if signature is cryptographically valid, {@code false} otherwise
     */
    public boolean verify(String data, String base64Signature, PublicKey publicKey) {
        if (data == null || base64Signature == null || publicKey == null) {
            logger.warn("Signature verification called with null argument(s) — returning false");
            return false;
        }
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean valid = verifier.verify(signatureBytes);
            logger.debug("Signature verification result: {}", valid);
            return valid;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.warn("Signature verification failed with exception: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Base64 signature data: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Public Key Utilities
    // -------------------------------------------------------------------------

    /**
     * Encode a {@link PublicKey} to a Base64 string (X.509 DER format).
     *
     * <p>The encoded form is safe to store in a database text column and can be
     * shared openly. It contains NO private key material.
     *
     * @param publicKey the RSA public key
     * @return Base64-encoded X.509 DER representation
     */
    public String encodePublicKey(PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey must not be null");
        }
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Decode a Base64-encoded public key string back into a {@link PublicKey}.
     *
     * @param base64PublicKey Base64-encoded public key (X.509 DER)
     * @return reconstructed {@link PublicKey}
     * @throws InvalidKeySpecException if the encoded key is malformed
     */
    public PublicKey decodePublicKey(String base64PublicKey) throws InvalidKeySpecException {
        if (base64PublicKey == null || base64PublicKey.isEmpty()) {
            throw new IllegalArgumentException("Encoded public key string must not be null or empty");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    /**
     * Compute a short SHA-256 fingerprint of a public key, formatted as a
     * colon-delimited hex string (first 8 bytes / 16 hex chars shown for brevity).
     *
     * <p>This fingerprint is safe to display in the UI and lets users confirm
     * which institution key signed a certificate without exposing the full key.
     *
     * <p>Format example: {@code A3:F1:2C:88:...} (16 hex pairs)
     *
     * @param publicKey the RSA public key to fingerprint
     * @return short colon-delimited fingerprint string
     */
    public String getPublicKeyFingerprint(PublicKey publicKey) {
        if (publicKey == null) {
            return "N/A";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            // Show first 16 bytes (128-bit prefix) formatted as XX:XX:... pairs
            StringBuilder sb = new StringBuilder();
            int displayBytes = Math.min(16, hash.length);
            for (int i = 0; i < displayBytes; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Return the full 64-hex-char SHA-256 fingerprint of a public key.
     *
     * @param publicKey the RSA public key
     * @return full 64-hex-character fingerprint
     */
    public String getFullPublicKeyFingerprint(PublicKey publicKey) {
        if (publicKey == null) {
            return "N/A";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
