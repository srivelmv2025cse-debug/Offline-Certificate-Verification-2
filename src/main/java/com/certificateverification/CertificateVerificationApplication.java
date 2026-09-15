package com.certificateverification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Offline Certificate Verification Using Blockchain application.
 *
 * <p>This application provides a system for issuing and verifying academic and
 * professional certificates using blockchain technology. It supports offline
 * verification through QR codes and digital signatures.</p>
 *
 * <p>Day 1: Basic project setup and homepage.</p>
 */
@SpringBootApplication
public class CertificateVerificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificateVerificationApplication.class, args);
    }
}
