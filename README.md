# Offline Certificate Verification Using Blockchain

A college Java project demonstrating how blockchain technology can be used to issue and verify academic and professional certificates — with full offline support.

---

## Project Objective

Traditional certificate verification relies on centralized databases that require an active Internet connection. This project solves that problem by:

- Storing certificate hashes on a **local blockchain**, making records tamper-proof.
- Embedding certificate data in **cryptographic QR codes** that can be verified without Internet access.
- Using **RSA digital signatures** to authenticate the issuing institution *(Upcoming)*.
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
| RSA Digital Signatures | Certificate authenticity *(Upcoming)* |
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
│   │   │   │   ├── HomeController.java                  ← Serves HTML pages (Home, Issue, Verify, QR Verify)
│   │   │   │   ├── CertificateController.java           ← REST API endpoints (/api/certificates/**)
│   │   │   │   ├── BlockchainController.java            ← Blockchain REST APIs (/api/blockchain/**)
│   │   │   │   └── QRController.java                    ← QR code REST APIs (/api/qr/**)
│   │   │   ├── dto/
│   │   │   │   ├── CertificateVerificationRequest.java  ← Canonical verification request DTO
│   │   │   │   └── CertificateVerificationResponse.java ← Structured verification result DTO
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
│   │   │   │   ├── Certificate.java                     ← Certificate entity (includes qrCodeData)
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
│   │   │   │   └── DigitalSignatureManager.java         ← RSA signatures
│   │   │   ├── offline/
│   │   │   │   └── OfflineVerificationManager.java      ← Offline verification manager
│   │   │   └── config/
│   │   │       ├── WebConfig.java                       ← MVC configuration
│   │   │       └── DatabaseConfig.java                  ← Database configuration
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── index.html                           ← Homepage with quick verification actions
│   │       │   ├── issue.html                           ← Certificate issuance form
│   │       │   ├── verify.html                          ← Canonical certificate verification page
│   │       │   ├── qr-verify.html                       ← QR code certificate verification page
│   │       │   ├── certificates.html                    ← Certificate management table
│   │       │   ├── certificate-details.html             ← Certificate detail & QR code display
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
│           └── QRVerificationIntegrationTests.java
├── pom.xml                                              ← Maven configuration
└── README.md
```

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
- [x] **Full Automated Test Suite**:
  - 39 passing tests across all test suites, including 12 dedicated tests in `QRVerificationIntegrationTests`.

---

## REST API Specification

### 1. Verify QR Code

**Endpoint:** `POST /api/qr/verify`

**Request Body (Scanned QR):**
```json
{
  "qrValue": "{\"certId\":\"CERT-2024-001\",\"ref\":\"4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17\"}"
}
```

**Request Body (Manual ID):**
```json
{
  "certificateId": "CERT-2024-001"
}
```

**Response (`200 OK` - Genuine):**
```json
{
  "result": "Genuine",
  "status": "GENUINE",
  "certificateId": "CERT-2024-001",
  "student": "John Doe",
  "studentName": "John Doe",
  "institution": "State University",
  "institutionName": "State University",
  "course": "B.Tech Computer Science",
  "courseName": "B.Tech Computer Science",
  "issueDate": "2024-06-01",
  "blockchainHash": "4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17",
  "qrReference": "4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17",
  "blockchainMatch": true,
  "verified": true,
  "message": "Certificate verified as genuine against the local blockchain.",
  "timestamp": "2024-06-01T10:00:00"
}
```

**Response (`200 OK` - Tampered):**
```json
{
  "result": "Tampered",
  "status": "TAMPERED",
  "certificateId": "CERT-2024-001",
  "blockchainMatch": false,
  "verified": false,
  "message": "Tampering detected! The QR verification reference does not match the immutable blockchain ledger."
}
```

**Response (`200 OK` - Revoked):**
```json
{
  "result": "Revoked",
  "status": "REVOKED",
  "certificateId": "CERT-2024-001",
  "verified": false,
  "message": "Certificate has been formally revoked by the issuing authority."
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
# 1. Build project and run all 39 tests
mvn clean test

# 2. Run the application
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

### Available Pages

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage with quick actions |
| `http://localhost:8080/verify-qr` | **QR Code Certificate Verification (Day 5)** |
| `http://localhost:8080/verify` | Data Verification (Canonical Hashing) |
| `http://localhost:8080/issue` | Issue Certificate form (auto-generates QR) |
| `http://localhost:8080/certificates` | Certificate Management (All Certificates) |
| `http://localhost:8080/certificate/{id}` | Certificate Details with QR code display & download |
| `http://localhost:8080/blockchain` | Blockchain Explorer & Chain Validator |
| `http://localhost:8080/about` | About page |

### Available REST APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/qr/verify` | **Verify certificate via QR code or manual ID (Day 5)** |
| `POST` | `/api/qr/generate/{id}` | **Generate QR code for certificate (Day 5)** |
| `GET` | `/api/qr/{id}` | **Get QR code data & payload (Day 5)** |
| `POST` | `/api/qr/decode` | **Decode QR image to text (Day 5)** |
| `POST` | `/api/certificates/verify` | Verify certificate credentials via canonical hash |
| `GET` | `/api/certificates` | List all certificates |
| `POST` | `/api/certificates` | Issue a certificate (auto-generates blockchain block & QR) |
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

---

*College Project · Java 21 · Spring Boot · Blockchain*
