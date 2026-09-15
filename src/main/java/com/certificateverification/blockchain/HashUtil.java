package com.certificateverification.blockchain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility for hashing certificate data using SHA-256.
 * Used to create tamper-proof fingerprints stored on the blockchain.
 *
 * <p>Day 1: Placeholder - actual MessageDigest implementation in Day 2.</p>
 */
@Component
public class HashUtil {

    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);

    /** The hashing algorithm to use. */
    public static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Placeholder: Generate a SHA-256 hash of the given input.
     * Full implementation in Day 2.
     *
     * @param input the string to hash
     * @return hex-encoded SHA-256 hash
     */
    public String sha256(String input) {
        logger.debug("HashUtil.sha256() - placeholder for Day 2");
        return "0".repeat(64); // 64-char placeholder
    }

    /**
     * Placeholder: Generate a combined hash from certificate fields.
     * Full implementation in Day 2.
     *
     * @param certificateId  the certificate unique ID
     * @param holderName     the holder's name
     * @param institutionName the issuing institution
     * @param courseName     the course/degree name
     * @param issueDate      the issue date as string
     * @return hex-encoded SHA-256 hash
     */
    public String hashCertificate(String certificateId,
                                   String holderName,
                                   String institutionName,
                                   String courseName,
                                   String issueDate) {
        logger.debug("HashUtil.hashCertificate() - placeholder for Day 2");
        String combined = certificateId + holderName + institutionName + courseName + issueDate;
        return sha256(combined);
    }
}
