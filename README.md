# Offline Certificate Verification Using Blockchain

A college Java project demonstrating how blockchain technology can be used to issue and verify academic and professional certificates — with full offline support.

---

## Project Objective

Traditional certificate verification relies on centralized databases that require an active Internet connection. This project solves that problem by:

- Storing certificate hashes on a **local blockchain**, making records tamper-proof.
- Embedding certificate data in **QR codes** that can be verified without Internet access *(Upcoming)*.
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
| ZXing | QR code generation and scanning *(Upcoming)* |
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
│   │   │   │   ├── HomeController.java                  ← Serves HTML pages (Home, Issue, Verify, Blockchain)
│   │   │   │   ├── CertificateController.java           ← REST API endpoints (/api/certificates/**)
│   │   │   │   └── BlockchainController.java            ← Blockchain REST APIs (/api/blockchain/**)
│   │   │   ├── dto/
│   │   │   │   ├── CertificateVerificationRequest.java  ← Verification request DTO
│   │   │   │   └── CertificateVerificationResponse.java ← Structured verification result DTO
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
│   │   │   │   └── DigitalSignatureManager.java         ← RSA signatures
│   │   │   ├── qr/
│   │   │   │   └── QRCodeGenerator.java                 ← QR code generation
│   │   │   ├── offline/
│   │   │   │   └── OfflineVerificationManager.java      ← Offline verification
│   │   │   └── config/
│   │   │       ├── WebConfig.java                       ← MVC configuration
│   │   │       └── DatabaseConfig.java                  ← Database configuration
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── index.html                           ← Homepage with quick verification action
│   │       │   ├── issue.html                           ← Certificate issuance form
│   │       │   ├── verify.html                          ← Certificate verification & fraud detection page
│   │       │   ├── certificates.html                    ← Certificate management table
│   │       │   ├── certificate-details.html             ← Certificate detail & verify link
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
│           └── CertificateVerificationIntegrationTests.java
├── pom.xml                                              ← Maven configuration
└── README.md
```

---

## Day 4 Implementation - Certificate Verification and Fraud Detection

Day 4 delivers robust, cryptographic verification and fraud/tamper detection using the local blockchain ledger:

### Key Features Implemented:
- [x] **Verify Certificate Page (`/verify`)**:
  - Light, responsive, user-friendly UI allowing entry of:
    - **Certificate ID**
    - **Student Name**
    - **Course Name**
    - **Institution**
    - **Issue Date**
- [x] **Identical Canonical SHA-256 Hashing**:
  - Uses the exact deterministic canonical format as certificate issuance:
    `id:<id>|student:<student>|course:<course>|institution:<institution>|type:<type>|issueDate:<date>|expiryDate:<expiry>`
  - SHA-256 generated directly via standard `java.security.MessageDigest`.
- [x] **Blockchain Ledger Search & Verification Logic**:
  1. **Certificate ID not found**:
     `Result = "Certificate Not Found / Potentially Fake"`
  2. **Certificate ID found and hash matches**:
     `Result = "GENUINE CERTIFICATE"`
  3. **Certificate ID found but hash does not match**:
     `Result = "TAMPERED CERTIFICATE"`
  4. **Certificate is revoked**:
     `Result = "REVOKED CERTIFICATE"`
- [x] **Clear Verification Result Display**:
  - Color-coded verdict banner: Green (Genuine), Red (Tampered), Orange/Amber (Revoked), Slate (Not Found)
  - Detailed comparison table showing:
    - **Certificate ID**
    - **Student**
    - **Institution**
    - **Course**
    - **Blockchain Hash**
    - **Calculated Hash**
    - **Blockchain Match** (`MATCH` / `MISMATCH`)
    - **Certificate Status** (`ISSUED` / `REVOKED` / `TAMPERED` / `NOT_FOUND`)
- [x] **REST API Endpoint (`POST /api/certificates/verify`)**:
  - Structured JSON request & response.
- [x] **Audit Logging**:
  - Every verification attempt is automatically recorded in the `audit_logs` SQLite table with certificate ID, action (`VERIFIED` / `FAILED_VERIFICATION`), requester IP address, timestamp, and calculated vs blockchain hash details.
- [x] **Automated Tests**:
  - 27 passing tests across all test suites, including thorough verification tests for genuine, modified/tampered, missing, and revoked scenarios.

---

## REST API Specification

### 1. Verify Certificate

**Endpoint:** `POST /api/certificates/verify`

**Request Body:**
```json
{
  "certificateId": "CERT-2024-001",
  "studentName": "John Doe",
  "courseName": "B.Tech Computer Science",
  "institutionName": "State University",
  "issueDate": "2024-06-01"
}
```

**Response (`200 OK` - Genuine):**
```json
{
  "result": "GENUINE CERTIFICATE",
  "certificateId": "CERT-2024-001",
  "student": "John Doe",
  "studentName": "John Doe",
  "institution": "State University",
  "institutionName": "State University",
  "course": "B.Tech Computer Science",
  "courseName": "B.Tech Computer Science",
  "issueDate": "2024-06-01",
  "blockchainHash": "4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17",
  "calculatedHash": "4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17",
  "blockchainMatch": true,
  "certificateStatus": "ISSUED",
  "verified": true,
  "message": "Certificate is authentic and matches the blockchain ledger.",
  "timestamp": "2024-06-01T10:00:00"
}
```

**Response (`200 OK` - Tampered):**
```json
{
  "result": "TAMPERED CERTIFICATE",
  "certificateId": "CERT-2024-001",
  "student": "Fake Imposter",
  "studentName": "Fake Imposter",
  "institution": "State University",
  "institutionName": "State University",
  "course": "B.Tech Computer Science",
  "courseName": "B.Tech Computer Science",
  "issueDate": "2024-06-01",
  "blockchainHash": "4e6dc2ce6b5a3bb42312009529fea381672063ae60d72720560c93b6dbc05a17",
  "calculatedHash": "9b12a88487b32c...",
  "blockchainMatch": false,
  "certificateStatus": "TAMPERED",
  "verified": false,
  "message": "Certificate data does not match the immutable hash recorded on the blockchain.",
  "timestamp": "2024-06-01T10:00:00"
}
```

**Response (`200 OK` - Revoked):**
```json
{
  "result": "REVOKED CERTIFICATE",
  "certificateId": "CERT-2024-001",
  "certificateStatus": "REVOKED",
  "verified": false,
  "message": "Certificate has been formally revoked by the issuing authority."
}
```

**Response (`200 OK` - Not Found / Fake):**
```json
{
  "result": "Certificate Not Found / Potentially Fake",
  "certificateId": "CERT-UNKNOWN",
  "blockchainHash": "N/A",
  "blockchainMatch": false,
  "certificateStatus": "NOT_FOUND",
  "verified": false,
  "message": "Certificate ID 'CERT-UNKNOWN' was not found on the blockchain ledger."
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
# 1. Build the project and run tests
mvn clean test

# 2. Run the application
mvn spring-boot:run
```

The application starts on **http://localhost:8080**

### Available Pages

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage with quick actions |
| `http://localhost:8080/verify` | **Verify Certificate (Day 4)** |
| `http://localhost:8080/issue` | Issue Certificate form |
| `http://localhost:8080/certificates` | Certificate Management (All Certificates) |
| `http://localhost:8080/certificate/{id}` | Certificate Details page with blockchain hash & verify button |
| `http://localhost:8080/blockchain` | Blockchain Explorer & Chain Validator |
| `http://localhost:8080/about` | About page |

### Available REST APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/certificates/verify` | **Verify certificate and detect tampering/fraud (Day 4)** |
| `GET` | `/api/certificates` | List all certificates |
| `POST` | `/api/certificates` | Issue a certificate (anchored to blockchain) |
| `GET` | `/api/certificates/{id}` | Get certificate by Certificate ID |
| `POST` | `/api/certificates/{id}/revoke` | Revoke certificate |
| `GET` | `/api/certificates/health` | Certificate API health check |
| `GET` | `/api/blockchain` | Get all blockchain blocks |
| `POST` | `/api/blockchain/add` | Add a new block to blockchain |
| `GET` | `/api/blockchain/validate` | Validate blockchain integrity |

---

## Planned Features (Day 5+)

- [ ] RSA digital signatures
- [ ] QR code generation with ZXing
- [ ] Offline verification via QR scan
- [ ] Audit trail dashboard UI

---

## Git Commit History

| Commit | Description |
|---|---|
| `Day 1` | Project setup and basic UI |
| `Day 2` | Certificate issuing module |
| `Day 3` | Java blockchain implementation |
| `Day 4` | Certificate verification and fraud detection |

---

*College Project · Java 21 · Spring Boot · Blockchain*
