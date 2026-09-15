package com.certificateverification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Day 1 smoke test — verifies that the Spring Boot application context
 * loads without errors.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:test_certificate_verification.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CertificateVerificationApplicationTests {

    /**
     * If this test passes, the application context loads successfully,
     * confirming that all beans are wired correctly.
     */
    @Test
    void contextLoads() {
        // No assertions needed — context loading is the test.
    }
}
