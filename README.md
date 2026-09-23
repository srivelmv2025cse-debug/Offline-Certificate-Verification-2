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
│   │   │   │   ├── HomeController.java                  ← Serves HTML pages (including /audit, /revoke)
│   │   │   │   ├── CertificateController.java           ← REST API endpoints (/api/certificates/**)
│   │   │   │   ├── AuditLogController.java              ← Day 7: Audit REST APIs (/api/audit-logs/**)
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
│   │   │   │   ├── AuditLogService.java                 ← Day 7: Audit log management & queries
│   │   │   │   └── BlockchainService.java               ← Blockchain operations
│   │   │   ├── model/
│   │   │   │   ├── Certificate.java                     ← Certificate entity (status, reason, timestamp)
│   │   │   │   ├── Block.java                           ← Blockchain block entity
│   │   │   │   └── AuditLog.java                        ← Day 7: Full Audit log entity
│   │   │   ├── repository/
│   │   │   │   ├── CertificateRepository.java           ← Certificate DB access (search & status queries)
│   │   │   │   ├── BlockRepository.java                 ← Blockchain DB access
│   │   │   │   └── AuditLogRepository.java              ← Day 7: Audit log DB access
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
│   │       │   ├── certificates.html                    ← Certificate management & search
│   │       │   ├── certificate-details.html             ← Certificate detail & revocation display
│   │       │   ├── revoke.html                          ← Day 7: Certificate revocation portal
│   │       │   ├── audit.html                           ← Day 7: Audit trail table UI
│   │       │   ├── blockchain.html                      ← Blockchain explorer & validator
│   │       │   ├── about.html                           ← Project overview
│   │       │   └── error.html                           ← Error page
│   │       ├── static/
│   │       │   ├── css/style.css                        ← Stylesheet (light theme & status badges)
│   │       │   └── js/main.js                           ← Frontend JavaScript
│   │       └── application.properties                   ← App configuration
│   └── test/
│       └── java/com/certificateverification/
│           ├── CertificateVerificationApplicationTests.java
│           ├── CertificateManagementIntegrationTests.java
│           ├── BlockchainIntegrationTests.java
│           ├── CertificateVerificationIntegrationTests.java
│           ├── QRVerificationIntegrationTests.java
│           ├── DigitalSignatureIntegrationTests.java
│           └── RevocationAndAuditIntegrationTests.java   ← Day 7
├── pom.xml                                              ← Maven configuration
└── README.md
```

---

## Day 7 Implementation - Certificate Revocation and Audit Trail

Day 7 delivers a comprehensive revocation management system and an immutable audit trail:

### Key Features Implemented:

- [x] **Certificate Revocation System**:
  - Institutions/admins can search certificates by Certificate ID, Student Name, or Institution.
  - Dedicated **Revocation Portal** (`/revoke`) allows finding certificates, inspecting credentials, and executing revocation.
  - Mandatory or documented **Revocation Reason** entered during revocation (with quick reason suggestions).
  - Exact **Revocation Timestamp** (`LocalDateTime`) recorded and persisted on the certificate entity.

- [x] **Certificate Status Lifecycle**:
  - **`VALID`**: Certificate is active, unrevoked, and unexpired.
  - **`REVOKED`**: Certificate has been formally revoked by the issuing authority.
  - **`EXPIRED`**: Certificate has passed its specified expiry date.
  - Strict Rule: **A revoked certificate is NEVER reported as genuine** across web forms, REST APIs, or QR verification.
  - Verification strictly returns `REVOKED CERTIFICATE` with `verified=false` and audit action `FAILED_VERIFICATION`.

- [x] **Audit Trail System (`AuditLog`)**:
  - Fully structured model with: `id`, `certificateId`, `action`, `timestamp`, `result`, `details` (and caller `ipAddress`).
  - Automatically records all critical actions:
    1. **Certificate Issuance**: `action=CERTIFICATE_ISSUANCE`, `result=SUCCESS`
    2. **Certificate Verification**: `action=VERIFIED`, `result=SUCCESS`
    3. **Failed Verification**: `action=FAILED_VERIFICATION`, `result=FAILED`
    4. **Revocation**: `action=REVOCATION`, `result=SUCCESS`
    5. **QR Verification**: `action=VERIFIED_QR` / `FAILED_QR_VERIFICATION`, `result=SUCCESS/FAILED`
    6. **Blockchain Validation**: `action=BLOCKCHAIN_VALIDATION`, `result=VALID/TAMPERED`

- [x] **Audit Trail Page (`audit.html` / `/audit`)**:
  - Clean, light, responsive table displaying:
    `Timestamp | Certificate ID | Action | Result`
  - Quick filters for actions (Issuance, Verified Genuine, Failed Verification, Revocation, QR Verification, Blockchain Validation).
  - Search input to filter audit logs by Certificate ID.

- [x] **Audit Trail REST APIs (`AuditLogController`)**:
  - `GET /api/audit-logs`: List all audit logs, with optional `?certificateId=...` and `?action=...` filtering.
  - `GET /api/audit-logs/{id}`: Retrieve specific audit log by ID.
  - `GET /api/audit-logs/certificate/{certificateId}`: Retrieve all logs for a certificate.
  - `POST /api/certificates/{id}/revoke`: Revoke certificate with JSON body `{"reason": "..."}` or query param.
  - `GET /api/certificates/search?query=...`: Search certificates.

- [x] **Integration Test Suite (`RevocationAndAuditIntegrationTests.java`)**:
  - 15 comprehensive tests covering revocation with reason & timestamp, status lifecycle, verification rejections, issuance/revocation/blockchain audit logging, REST APIs, and UI views.
  - Total: **74 tests passing** across all test suites.

---

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

## Day 8 Implementation — Offline Verification & Blockchain Synchronization

Day 8 delivers a **realistic, Internet-independent offline certificate verification** system. Certificates can now be verified without any network access using a locally cached blockchain snapshot.

### Architecture

```
ONLINE (Institution Side)              OFFLINE (Field Verifier Side)
┌──────────────────────────┐            ┌──────────────────────────────┐
│ Live SQLite blockchain   │            │ Local blockchain snapshot    │
│ Institution public keys  │ ──JSON──►  │ (offline_sync/*.json)        │
│ Certificate database     │            │          ↓                   │
└──────────────────────────┘            │ OfflineVerificationService   │
                                        │          ↓                   │
                                        │ GENUINE / TAMPERED / REVOKED │
                                        │ EXPIRED  / NOT_FOUND         │
                                        └──────────────────────────────┘
```

### Key Features Implemented

- [x] **Offline Sync Package Generation (`SyncService`)**:
  - Exports all blockchain blocks, certificate records, and Base64-encoded RSA-2048 institution public keys to a self-contained JSON snapshot file (`offline_sync/blockchain_snapshot.json`).
  - Snapshot auto-loaded on application startup if file exists.
- [x] **Offline Blockchain Cache (`OfflineBlockchainService`)**:
  - In-memory copy of the exported blockchain with the same SHA-256 hash chain logic as the live `Blockchain` class.
  - `isChainValid()` verifies genesis block, sequential block indices, each block's hash, and hash chain linkage — 100% cryptographic verification.
- [x] **Offline Verification Service (`OfflineVerificationService`)**:
  - Performs full 3-factor verification using **zero** online API calls or database queries:
    1. **Blockchain integrity**: Recalculates SHA-256 canonical hash and compares to the offline block record.
    2. **RSA digital signature**: Verifies issuer's signature using institution public key from the offline cache.
    3. **Revocation/expiry**: Checks offline certificate status record.
  - Returns structured `OfflineVerificationResult` with `GENUINE | TAMPERED | REVOKED | EXPIRED | NOT_FOUND`.
- [x] **Offline Verification Manager (`OfflineVerificationManager`)**:
  - Delegates to `OfflineVerificationService` for certificate-ID-based offline checks.
  - Supports JSON snapshot export/import as string for programmatic use.
- [x] **Sync Status Dashboard (`/sync-status`)**:
  - Shows block count, certificate count, revoked count, and verifier readiness.
  - Buttons to Generate Snapshot, Import/Reload Snapshot, and validate offline chain integrity.
- [x] **Offline Verify Page (`/offline-verify`)**:
  - Amber "⚡ OFFLINE MODE" banner indicating disconnected operation.
  - Certificate ID input with optional field overrides for tamper testing.
  - Color-coded result banners: Genuine (green), Tampered (red), Revoked (orange), Expired (yellow), Not Found (grey).
- [x] **Navigation Updated**: All 9 existing pages now include "Offline Verify" and "Sync Status" links.
- [x] **Integration Tests (`OfflineVerificationIntegrationTests`)**: 14 tests covering sync generation, import, GENUINE, TAMPERED (field override), REVOKED, EXPIRED, NOT_FOUND, chain integrity, manager delegation, and all 4 UI endpoints.

### Offline Sync Package Format (`blockchain_snapshot.json`)

```json
{
  "generatedAt": "2024-09-22T05:10:00Z",
  "version": "1.0",
  "totalBlocks": 12,
  "totalCertificates": 11,
  "totalRevokedCertificates": 2,
  "blockchainBlocks": [
    {
      "index": 0,
      "timestamp": 1726924200000,
      "certificateId": "GENESIS",
      "certificateHash": "0000000000000000",
      "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
      "hash": "f273c1970da757..."
    }
  ],
  "certificates": [
    {
      "certificateId": "CERT-2024-001",
      "studentName": "Jane Doe",
      "courseName": "B.Tech Computer Science",
      "institutionName": "State University",
      "certificateType": "Degree",
      "issueDate": "2024-06-01",
      "expiryDate": "",
      "status": "VALID",
      "blockchainHash": "abc123...",
      "digitalSignature": "<Base64-RSA-sig>",
      "revoked": false
    }
  ],
  "publicKeys": {
    "state university": "<Base64-DER-RSA-2048-public-key>"
  }
}
```

---

## How to Run

### Prerequisites

- Java 21 (JDK) installed
- Maven 3.8+ installed
- Git installed

### Steps

```bash
# 1. Build project and run all 88 tests
mvn clean test

# 2. Run the application
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

### Available Pages

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage with quick actions |
| `http://localhost:8080/verify` | Certificate Verification (3-Factor: Hash + Signature + Revocation) |
| `http://localhost:8080/verify-qr` | QR Code Certificate Verification |
| `http://localhost:8080/issue` | Issue Certificate form (auto-generates QR & digital signature) |
| `http://localhost:8080/certificates` | Certificate Management with search & status indicators |
| `http://localhost:8080/certificate/{id}` | Certificate Details with QR code, signature, and revocation status |
| `http://localhost:8080/revoke` | Certificate Revocation Portal (search & revoke with reason) |
| `http://localhost:8080/audit` | Audit Trail (timestamp, cert ID, action, result) |
| `http://localhost:8080/blockchain` | Blockchain Explorer & Chain Validator |
| `http://localhost:8080/offline-verify` | **Day 8: Offline Certificate Verification (no Internet required)** |
| `http://localhost:8080/sync-status` | **Day 8: Blockchain Sync Status Dashboard (generate & import snapshot)** |
| `http://localhost:8080/about` | About page |

### Available REST APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/certificates/verify` | Verify certificate (3-factor: hash + signature + revocation) |
| `POST` | `/api/certificates/{id}/revoke` | Revoke certificate with optional reason |
| `GET` | `/api/certificates/search` | Search certificates by ID, student, or institution |
| `GET` | `/api/audit-logs` | Retrieve all audit trail entries with optional filtering |
| `GET` | `/api/audit-logs/{id}` | Retrieve specific audit log entry by ID |
| `GET` | `/api/audit-logs/certificate/{id}` | Retrieve audit logs for a specific certificate |
| `POST` | `/api/qr/verify` | Verify certificate via QR code or manual ID |
| `POST` | `/api/qr/generate/{id}` | Generate QR code for certificate |
| `GET` | `/api/qr/{id}` | Get QR code data & payload |
| `POST` | `/api/qr/decode` | Decode QR image to text |
| `GET` | `/api/certificates` | List all certificates |
| `POST` | `/api/certificates` | Issue a certificate (auto-generates blockchain block, QR & signature) |
| `GET` | `/api/certificates/{id}` | Get certificate by Certificate ID |
| `GET` | `/api/blockchain` | Get all blockchain blocks |
| `POST` | `/api/blockchain/add` | Add a new block to blockchain |
| `GET` | `/api/blockchain/validate` | Validate blockchain integrity (logs to audit trail) |

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
| `Day 7` | Revocation and audit trail |
| `Day 8` | Offline verification and blockchain synchronization |

---

*College Project · Java 21 · Spring Boot · Blockchain · RSA Digital Signatures · Offline Verification*



