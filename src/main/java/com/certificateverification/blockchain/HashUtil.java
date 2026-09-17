package com.certificateverification.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic utility for hashing certificate and block data using SHA-256.
 * Implements hashing completely using standard Java MessageDigest.
 */
@Component
public class HashUtil {

    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);

    /** The hashing algorithm to use. */
    public static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Compute a SHA-256 hash of the given input string using Java MessageDigest.
     *
     * @param input the input string to hash
     * @return 64-character lowercase hexadecimal SHA-256 hash
     */
    public static String sha256(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available in JVM", e);
            throw new IllegalStateException("SHA-256 algorithm is not supported by this JVM", e);
        }
    }

    /**
     * Generate a canonical representation of certificate data and return its SHA-256 hash.
     *
     * @param certificateId   unique certificate identifier
     * @param studentName     student / holder name
     * @param courseName      course / program name
     * @param institutionName issuing institution
     * @param certificateType certificate type
     * @param issueDate       issue date
     * @param expiryDate      expiry date (nullable)
     * @return hex-encoded SHA-256 hash of canonical representation
     */
    public static String hashCertificate(String certificateId,
                                         String studentName,
                                         String courseName,
                                         String institutionName,
                                         String certificateType,
                                         String issueDate,
                                         String expiryDate) {
        String canonical = buildCanonicalCertificateString(
                certificateId, studentName, courseName, institutionName, certificateType, issueDate, expiryDate);
        return sha256(canonical);
    }

    /**
     * Overloaded helper without expiryDate.
     */
    public static String hashCertificate(String certificateId,
                                         String studentName,
                                         String courseName,
                                         String institutionName,
                                         String certificateType,
                                         String issueDate) {
        return hashCertificate(certificateId, studentName, courseName, institutionName, certificateType, issueDate, null);
    }

    /**
     * Generate deterministic canonical string for certificate fields.
     */
    public static String buildCanonicalCertificateString(String certificateId,
                                                         String studentName,
                                                         String courseName,
                                                         String institutionName,
                                                         String certificateType,
                                                         String issueDate,
                                                         String expiryDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(certificateId != null ? certificateId.trim() : "")
          .append("|student:").append(studentName != null ? studentName.trim() : "")
          .append("|course:").append(courseName != null ? courseName.trim() : "")
          .append("|institution:").append(institutionName != null ? institutionName.trim() : "")
          .append("|type:").append(certificateType != null ? certificateType.trim() : "")
          .append("|issueDate:").append(issueDate != null ? issueDate.trim() : "");
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            sb.append("|expiryDate:").append(expiryDate.trim());
        }
        return sb.toString();
    }
}
