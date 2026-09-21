# Offline Certificate Verification Using Blockchain

A college Java project demonstrating how blockchain technology can be used to issue and verify academic and professional certificates — with full offline support.

---

## Project Objective

Traditional certificate verification relies on centralized databases that require an active Internet connection. This project solves that problem by:

- Storing certificate hashes on a **local blockchain**, making records tamper-proof.
- Embedding certificate data in **cryptographic QR codes** that can be verified without Internet access.
- Using **RSA-2048 digital signatures** to authenticate the issuing institution.
- Providing a complete **audit trail** of all certificate actions including issuance, verification, and revocation.

---

## Technologies

| Technology | Purpose |
|---|---|
| Java 21 | Core programming language |
| Spring Boot 3.3 | Backend web framework (MVC + REST API) |
| Maven | Build tool and dependency management |
| SQLite | Lightweight embedded database (no server needed) |
| Hibernate / JPA | ORM for database access |
| Thymeleaf | Server-side HTML rendering |
| HTML / CSS / JavaScript | Frontend |
| ZXing 3.5.3 | QR code generation and decoding (PNG & Base64) |
| RSA-2048 / SHA256withRSA | Digital signatures for certificate authenticity |
| SHA-256 Hashing | Blockchain integrity & deterministic canonical data hashing |

---

## Project Structure

```
certificate-verification/
├── src/
│   ├── main/
│   │   ├── java/com/certificateverification/
│   │   │   ├── CertificateVerificationApplication.java  ← Main entry point
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java                  ← Serves HTML pages
│   │   │   │   ├── CertificateController.java           ← REST API endpoints (/api/certificates/**)
│   │   │   │   ├── BlockchainController.java            ← Blockchain REST APIs (/api/blockchain/**)
│   │   │   │   └── QRController.java                    ← QR code REST APIs (/api/qr/**)
│   │   │   ├── dto/
│   │   │   │   ├── CertificateVerificationRequest.java  ← Canonical verification request DTO
│   │   │   │   └── CertificateVerificationResponse.java ← Structured verification result DTO
│   │   │   ├── signature/                                ← Day 6: Digital Signature Package
│   │   │   │   ├── DigitalSignatureService.java         ← RSA-2048 key generation, signing & verification
│   │   │   │   └── InstitutionKeyStore.java             ← In-memory institution key pair store
│   │   │   ├── qr/
│   │   │   │   ├── QRCodeGenerator.java                 ← ZXing QR generator and decoder
│   │   │   │   ├── QRService.java                       ← QR generation, payload handling & verification
│   │   │   │   ├── QRVerificationPayload.java           ← Non-sensitive QR payload encoder/decoder
│   │   │   │   ├── QRVerificationRequest.java           ← QR verification request DTO
│   │   │   │   └── QRVerificationResponse.java          ← QR verification response DTO
│   │   │   ├── service/
│   │   │   │   ├── CertificateService.java              ← Issuance, verification & revocation logic
│   │   │   │   └── BlockchainService.java               ← Blockchain operations
│   │   │   ├── model/
│   │   │   │   ├── Certificate.java                     ← Certificate entity
│   │   │   │   ├── Block.java                           ← Blockchain block entity
│   │   │   │   └── AuditLog.java                        ← Audit log entity
│   │   │   ├── repository/
│   │   │   │   ├── CertificateRepository.java           ← Certificate DB access
│   │   │   │   ├── BlockRepository.java                 ← Blockchain DB access
│   │   │   │   └── AuditLogRepository.java              ← Audit log DB access
│   │   │   ├── blockchain/
│   │   │   │   ├── Blockchain.java                      ← Core in-memory & SQLite blockchain
│   │   │   │   ├── BlockchainManager.java               ← Blockchain manager facade
│   │   │   │   └── HashUtil.java                        ← SHA-256 canonical hashing utility
│   │   │   ├── security/
│   │   │   │   └── DigitalSignatureManager.java         ← Legacy security manager
│   │   │   ├── offline/
│   │   │   │   └── OfflineVerificationManager.java      ← Offline verification manager
│   │   │   └── config/
│   │   │       ├── WebConfig.java                       ← MVC configuration
│   │   │       └── DatabaseConfig.java                  ← Database configuration
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── index.html                           ← Homepage
│   │       │   ├── issue.html                           ← Certificate issuance form
│   │       │   ├── verify.html                          ← Certificate verification (3-factor)
│   │       │   ├── qr-verify.html                       ← QR code verification page
│   │       │   ├── certificates.html                    ← Certificate management table
│   │       │   ├── certificate-details.html             ← Certificate detail & QR & signature display
│   │       │   ├── blockchain.html                      ← Blockchain explorer & validator
│   │       │   ├── about.html                           ← Project overview
│   │       │   └── error.html                           ← Error page
│   │       ├── static/
│   │       │   ├── css/style.css                        ← Stylesheet
│   │       │   └── js/main.js                           ← Frontend JavaScript
│   │       └── application.properties                   ← App configuration
│   └── test/
│       └── java/com/certificateverification/
│           ├── CertificateVerificationApplicationTests.java
│           ├── CertificateManagementIntegrationTests.java
│           ├── BlockchainIntegrationTests.java
│           ├── CertificateVerificationIntegrationTests.java
│           ├── QRVerificationIntegrationTests.java
│           └── DigitalSignatureIntegrationTests.java     ← Day 6
├── pom.xml                                              ← Maven configuration
└── README.md
```

---

## Day 6 Implementation - Digital Signature Security

Day 6 adds RSA-2048 digital signatures to authenticate certificate issuance and strengthen verification to a full three-factor security model.

### Key Features Implemented:

- [x] **DigitalSignatureService** (`com.certificateverification.signature`):
  - RSA-2048 key pair generation using Java's built-in `KeyPairGenerator` and `SecureRandom`.
  - SHA256withRSA signature generation and verification using `java.security.Signature`.
  - Public key encoding/decoding (X.509 DER / Base64) for storage and retrieval.
  - Public key SHA-256 fingerprint generation (colon-delimited hex, first 16 bytes).
  - No external cryptographic libraries required — pure Java Security API (JCA).

- [x] **InstitutionKeyStore** (`com.certificateverification.signature`):
  - In-memory `ConcurrentHashMap` mapping normalised institution names to RSA key pairs.
  - Pre-seeded with 10 default institutions at `@PostConstruct` startup.
  - Lazy key pair creation for unknown institutions (auto-generated on first use).
  - Thread-safe concurrent access.

- [x] **Certificate Signing on Issuance**:
  - When a certificate is issued, the SHA-256 certificate hash is signed with the institution's RSA private key.
  - The Base64-encoded signature is stored in `certificate.digitalSignature` (TEXT column).

- [x] **Three-Factor Verification** (updated `CertificateService.verifyCertificate()`):
  - A certificate is **GENUINE** only when ALL three conditions are met:
    1. ✅ **Blockchain Hash Match** — Recalculated SHA-256 hash matches the immutable blockchain record.
    2. ✅ **Digital Signature Valid** — RSA signature is cryptographically verified against the institution's public key.
    3. ✅ **Not Revoked** — Certificate has not been formally revoked.
  - Failure of ANY factor results in TAMPERED, REVOKED, or NOT FOUND verdict.

- [x] **Updated Verification UI** (`verify.html`):
  - Three-factor security check summary chips (Blockchain Hash / Digital Signature / Not Revoked).
  - Dedicated Digital Signature panel showing:
    - Signature Status (VALID / INVALID / SIGNATURE MISSING / KEY NOT FOUND)
    - Issuing Institution name
    - Public Key Fingerprint (colon-delimited hex)
  - Security notice: private keys are never displayed or exposed via any endpoint.

- [x] **Certificate Details Page** (`certificate-details.html`):
  - Shows "🔑 RSA-2048 Signed" or "⚠ Not Signed" status for each certificate.

- [x] **Integration Test Suite** (`DigitalSignatureIntegrationTests.java`):
  - 16 dedicated tests covering key generation, sign/verify round-trip, tampering detection,
    key store seeding, lazy creation, issuance signature, and full 3-factor verification outcomes.
  - Total: **59 tests passing** across all test suites.

### Security Notice

> **⚠️ Educational Project — Key Management**
>
> In this project, RSA private keys are held in memory (InstitutionKeyStore) for simplicity.
> In a production system, private keys **MUST** be stored in:
> - A Hardware Security Module (HSM)
> - A Java KeyStore (.jks / PKCS#12) with strong passwords
> - A dedicated secrets manager (e.g., HashiCorp Vault, AWS KMS)
>
> Private keys are **NEVER** displayed in the UI, exposed via REST APIs, or persisted to the database.
> Only public key fingerprints are shown for institution identification.

---

## Day 5 Implementation - QR Code Certificate Verification

Day 5 delivers full cryptographic QR Code generation, non-sensitive credential encoding, and offline-capable verification:

### Key Features Implemented:
- [x] **ZXing QR Code Engine (`QRCodeGenerator`)**:
  - Encodes and decodes QR codes as PNG byte arrays and Base64 Data URIs (`data:image/png;base64,...`).
  - Supports decoding from camera uploads, image files, or raw Base64 data.
- [x] **Automatic QR Generation on Issuance**:
  - Automatically generates a QR code when a certificate is issued.
  - Stores the Base64 image in `certificate.qrCodeData`.
- [x] **Privacy-First Non-Sensitive QR Payload (`QRVerificationPayload`)**:
  - Encodes strictly the **Certificate ID** and the **blockchain verification reference** (SHA-256 hash).
  - **Does NOT expose** student name, course, institution, grades, or personal details in the QR code.
- [x] **Certificate Details QR Display (`certificate-details.html`)**:
  - Displays the generated QR code prominently alongside certificate credentials.
  - "Download QR" action to save the QR PNG.
  - "Regenerate QR" and "Verify Using QR" shortcut buttons.
- [x] **QR Verification Page (`qr-verify.html` / `/verify-qr`)**:
  - Light, clean, professional interface offering three verification modes:
    1. Enter/paste scanned QR value or JSON payload.
    2. Enter Certificate ID manually.
    3. Upload QR code image file (PNG/JPEG) for automated decoding.
- [x] **Simple, High-Level Verification Verdicts**:
  - **Genuine**: Verified against local blockchain ledger.
  - **Tampered**: QR reference or database hash does not match immutable blockchain block.
  - **Not Found**: Certificate ID not present on the blockchain ledger.
  - **Revoked**: Certificate has been formally revoked by issuer.
  - **Invalid QR Code**: Malformed, empty, or unreadable QR data.
- [x] **Offline Capability**:
  - Verification operates locally against the SQLite blockchain database without needing internet access.
- [x] **QR REST APIs (`QRController`)**:
  - `POST /api/qr/generate/{certificateId}`: Generate or regenerate QR code
  - `GET /api/qr/{certificateId}`: Retrieve QR code and payload
  - `POST /api/qr/verify`: Verify certificate via QR value or manual ID
  - `POST /api/qr/decode`: Decode image to QR text

---

## How to Run

### Prerequisites

- Java 21 (JDK) installed
- Maven 3.8+ installed
- Git installed

### Steps

```bash
# 1. Build project and run all 59 tests
mvn clean test

# 2. Run the application
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

### Available Pages

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage with quick actions |
| `http://localhost:8080/verify` | **Certificate Verification (3-Factor: Hash + Signature + Revocation)** |
| `http://localhost:8080/verify-qr` | QR Code Certificate Verification |
| `http://localhost:8080/issue` | Issue Certificate form (auto-generates QR & digital signature) |
| `http://localhost:8080/certificates` | Certificate Management (All Certificates) |
| `http://localhost:8080/certificate/{id}` | Certificate Details with QR code & signature status |
| `http://localhost:8080/blockchain` | Blockchain Explorer & Chain Validator |
| `http://localhost:8080/about` | About page |

### Available REST APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/certificates/verify` | **Verify certificate (3-factor: hash + signature + revocation) (Day 6)** |
| `POST` | `/api/qr/verify` | Verify certificate via QR code or manual ID |
| `POST` | `/api/qr/generate/{id}` | Generate QR code for certificate |
| `GET` | `/api/qr/{id}` | Get QR code data & payload |
| `POST` | `/api/qr/decode` | Decode QR image to text |
| `GET` | `/api/certificates` | List all certificates |
| `POST` | `/api/certificates` | Issue a certificate (auto-generates blockchain block, QR & signature) |
| `GET` | `/api/certificates/{id}` | Get certificate by Certificate ID |
| `POST` | `/api/certificates/{id}/revoke` | Revoke certificate |
| `GET` | `/api/blockchain` | Get all blockchain blocks |
| `POST` | `/api/blockchain/add` | Add a new block to blockchain |
| `GET` | `/api/blockchain/validate` | Validate blockchain integrity |

---

## Git Commit History

| Commit | Description |
|---|---|
| `Day 1` | Project setup and basic UI |
| `Day 2` | Certificate issuing module |
| `Day 3` | Java blockchain implementation |
| `Day 4` | Certificate verification and fraud detection |
| `Day 5` | QR code certificate verification |
| `Day 6` | Digital signature security |

---

*College Project · Java 21 · Spring Boot · Blockchain · RSA Digital Signatures*
