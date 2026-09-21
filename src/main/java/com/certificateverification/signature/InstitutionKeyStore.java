package com.certificateverification.signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 6 - Institution Key Store.
 *
 * <p>An in-memory store that maps institution names to their RSA key pairs.
 * Keys are generated on first use and kept in memory for the lifetime of
 * the application.
 *
 * <p><strong>IMPORTANT — Educational Project Notice:</strong>
 * <ul>
 *   <li>This in-memory approach is intentionally simple for a college demonstration.</li>
 *   <li>In production, private keys MUST be stored in a Hardware Security Module (HSM)
 *       or a Java KeyStore (.jks / PKCS#12) protected with a strong password and
 *       access controls. They must NEVER be accessible via an HTTP endpoint.</li>
 *   <li>Public keys may be stored in a database or LDAP for broad distribution.</li>
 *   <li>Key rotation policies should be enforced in production systems.</li>
 * </ul>
 *
 * <p>A set of well-known default institutions is pre-seeded at startup so that
 * certificates issued via the UI immediately have valid signing keys.
 */
@Component
public class InstitutionKeyStore {

    private static final Logger logger = LoggerFactory.getLogger(InstitutionKeyStore.class);

    /** Default institutions whose key pairs are seeded at startup. */
    public static final Set<String> DEFAULT_INSTITUTIONS = Set.of(
            "Default Institution",
            "State University",
            "National Institute of Technology",
            "Indian Institute of Technology",
            "Anna University",
            "Madras University",
            "Bharathiar University",
            "VIT University",
            "SRM University",
            "Amrita University"
    );

    /** Thread-safe map: normalised institution name → RSA key pair */
    private final Map<String, KeyPair> keyStore = new ConcurrentHashMap<>();

    private final DigitalSignatureService signatureService;

    @Autowired
    public InstitutionKeyStore(DigitalSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    /**
     * Seed default institution key pairs at application startup.
     * This ensures that newly issued certificates can always be signed.
     */
    @PostConstruct
    public void seedDefaultKeys() {
        logger.info("Seeding RSA key pairs for {} default institutions...", DEFAULT_INSTITUTIONS.size());
        for (String institution : DEFAULT_INSTITUTIONS) {
            KeyPair kp = signatureService.generateKeyPair();
            keyStore.put(normalise(institution), kp);
            logger.debug("Seeded key pair for institution: {}", institution);
        }
        logger.info("Institution key store seeding complete. {} keys loaded.", keyStore.size());
    }

    /**
     * Retrieve an existing key pair for the given institution, or generate and
     * store a new one if none exists (lazy registration).
     *
     * @param institutionName name of the issuing institution
     * @return existing or freshly generated {@link KeyPair}
     */
    public KeyPair getOrCreateKeyPair(String institutionName) {
        String key = normalise(institutionName);
        return keyStore.computeIfAbsent(key, k -> {
            logger.info("No key pair found for '{}' — generating new RSA-2048 key pair", institutionName);
            return signatureService.generateKeyPair();
        });
    }

    /**
     * Retrieve only the public key for a given institution.
     *
     * <p>This method is safe to call from any context including UI rendering —
     * it returns only the public portion of the key pair.
     *
     * @param institutionName name of the issuing institution
     * @return RSA {@link PublicKey}, or {@code null} if no key pair exists
     */
    public PublicKey getPublicKey(String institutionName) {
        KeyPair kp = keyStore.get(normalise(institutionName));
        return kp != null ? kp.getPublic() : null;
    }

    /**
     * Register an externally-provided key pair for a given institution.
     *
     * @param institutionName name of the institution
     * @param keyPair         RSA key pair to associate with the institution
     */
    public void registerKeyPair(String institutionName, KeyPair keyPair) {
        if (institutionName == null || keyPair == null) {
            throw new IllegalArgumentException("Institution name and key pair must not be null");
        }
        keyStore.put(normalise(institutionName), keyPair);
        logger.info("Key pair registered for institution: {}", institutionName);
    }

    /**
     * Check whether a key pair exists for the given institution.
     *
     * @param institutionName institution name to check
     * @return {@code true} if a key pair is registered
     */
    public boolean hasKeyPair(String institutionName) {
        return keyStore.containsKey(normalise(institutionName));
    }

    /**
     * Return the number of institutions currently registered in the key store.
     */
    public int size() {
        return keyStore.size();
    }

    /**
     * Normalise institution name for consistent key lookup:
     * trim whitespace and convert to lowercase.
     */
    private String normalise(String institutionName) {
        return institutionName != null ? institutionName.trim().toLowerCase() : "";
    }
}
