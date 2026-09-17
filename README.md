# Offline Certificate Verification Using Blockchain

A college Java project demonstrating how blockchain technology can be used to issue and verify academic and professional certificates — with full offline support.

---

## Project Objective

Traditional certificate verification relies on centralized databases that require an active Internet connection. This project solves that problem by:

- Storing certificate hashes on a **local blockchain**, making records tamper-proof.
- Embedding certificate data in **QR codes** that can be verified without Internet access.
- Using **RSA digital signatures** to authenticate the issuing institution.
- Providing a complete **audit trail** of all certificate actions.

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
| ZXing | QR code generation and scanning *(Day 2+)* |
| RSA Digital Signatures | Certificate authenticity *(Day 2+)* |
| SHA-256 Hashing | Blockchain integrity *(Day 2+)* |

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
│   │   │   │   └── CertificateController.java           ← REST API endpoints
│   │   │   ├── service/
│   │   │   │   ├── CertificateService.java              ← Business logic
│   │   │   │   └── BlockchainService.java               ← Blockchain operations
│   │   │   ├── model/
│   │   │   │   ├── Certificate.java                     ← Certificate entity
│   │   │   │   ├── Block.java                           ← Blockchain block entity
│   │   │   │   └── AuditLog.java                        ← Audit trail entity
│   │   │   ├── repository/
│   │   │   │   ├── CertificateRepository.java           ← Certificate DB access
│   │   │   │   ├── BlockRepository.java                 ← Blockchain DB access
│   │   │   │   └── AuditLogRepository.java              ← Audit log DB access
│   │   │   ├── blockchain/
│   │   │   │   ├── BlockchainManager.java               ← Core blockchain logic
│   │   │   │   └── HashUtil.java                        ← SHA-256 utility
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
│   │       │   ├── index.html                           ← Homepage
│   │       │   └── about.html                           ← About page
│   │       ├── static/
│   │       │   ├── css/style.css                        ← Stylesheet
│   │       │   └── js/main.js                           ← Frontend JavaScript
│   │       └── application.properties                   ← App configuration
│   └── test/
│       └── java/com/certificateverification/
│           └── CertificateVerificationApplicationTests.java
├── pom.xml                                              ← Maven configuration
└── README.md
```

---

## Day 1 Features

- [x] Complete Maven project structure (Java 21 + Spring Boot 3.3)
- [x] All package directories created with meaningful placeholder classes
- [x] SQLite database connection configured
- [x] JPA entities: `Certificate`, `Block`, `AuditLog`
- [x] Repository interfaces for all entities
- [x] Service layer stubs: `CertificateService`, `BlockchainService`
- [x] Blockchain components: `BlockchainManager`, `HashUtil`
- [x] Security component: `DigitalSignatureManager`
- [x] QR component: `QRCodeGenerator`
- [x] Offline component: `OfflineVerificationManager`
- [x] Spring MVC config (`WebConfig`)
- [x] Homepage with hero, features, how-it-works, and footer
- [x] About page
- [x] Responsive light-themed CSS
- [x] Mobile navigation with hamburger menu
- [x] Spring Boot smoke test

---

## Day 2 Features (Completed)

- [x] **Certificate Model**: Complete entity containing `id`, `certificateId`, `studentName`, `courseName`, `institutionName`, `certificateType`, `issueDate`, `expiryDate`, `status`, and `createdAt`.
- [x] **SQLite Database Persistence**: Hibernate table mapping for `certificates` with automatic timestamp lifecycle management.
- [x] **Certificate Repository**: `CertificateRepository` providing CRUD queries (`findByCertificateId`, `existsByCertificateId`, `findAllByOrderByCreatedAtDesc`).
- [x] **Certificate Service**: `CertificateService` with strict validation:
  - Required fields cannot be empty.
  - Certificate ID uniqueness check.
  - Issue date validation.
  - Expiry date cannot be before issue date.
- [x] **Issue Certificate Page (`/issue`)**: Clean light UI form with fields for ID, Student Name, Course, Institution, Type, Issue Date, Expiry Date.
- [x] **Certificate Details Page (`/certificate/{id}`)**: Dedicated details view with success notification: *"Certificate issued successfully."*
- [x] **Certificate Management Page (`/certificates`)**: Overview table listing all issued certificates with status badges (ISSUED, REVOKED), direct view action, and revocation capabilities.
- [x] **REST APIs**: Full JSON API support under `/api/certificates`:
  - `POST /api/certificates` — Issue a new certificate
  - `GET /api/certificates` — Retrieve all certificates
  - `GET /api/certificates/{certificateId}` — Retrieve specific certificate details
  - `POST /api/certificates/{certificateId}/revoke` — Revoke certificate
  - `GET /api/certificates/health` — Health check endpoint
- [x] **Integration & Unit Tests**: Full test coverage verifying models, services, form submission, validation constraints, and REST controllers.

---

---

## Day 3 Features (Completed)

- [x] **Custom Educational Blockchain in Java**: Built from scratch using core Java (no cryptocurrency or external blockchain dependencies).
- [x] **Block Model**: Complete `Block` class containing `index`, `timestamp`, `certificateId`, `certificateHash`, `previousHash`, and `hash`.
- [x] **Java MessageDigest Cryptography**: SHA-256 calculation implemented using standard `java.security.MessageDigest` in `HashUtil`.
- [x] **Blockchain Class (`Blockchain.java`)**:
  - Automatically initializes and creates the **Genesis Block** (Block #0).
  - Calculates SHA-256 hashes linking each block to the previous block's hash.
  - Adds new certificate blocks with sequential indexing.
  - Complete chain validation (`isChainValid()`) verifying cryptographic linkage and integrity.
  - Detects modified or tampered block contents and broken links.
  - Persistent SQLite storage via `BlockRepository`.
- [x] **Automated Certificate Anchoring on Issuance**:
  1. Generates deterministic canonical representation of certificate data.
  2. Calculates SHA-256 certificate hash.
  3. Creates a new blockchain block linked to the previous block.
  4. Stores certificate hash in the block and updates `blockchainHash` on the certificate entity.
- [x] **Blockchain Explorer Web UI (`/blockchain`)**:
  - Displays all blocks (Block number, Certificate ID, Certificate hash, Previous hash, Current hash, Timestamp).
  - Interactive **"Validate Blockchain"** button.
  - Real-time display of **"Blockchain Valid"** or **"Blockchain Tampered"**.
  - Clean, light, professional UI with monospace hash rendering.
- [x] **Blockchain REST APIs**:
  - `POST /api/blockchain/add` — Add a new block to the blockchain
  - `GET /api/blockchain` — Retrieve full blockchain blocks array
  - `GET /api/blockchain/validate` — Validate entire blockchain integrity
- [x] **Integration & Tamper Detection Tests**: 16 passing tests covering hashing, genesis creation, block linkage, tampering detection, and controller endpoints.

---

## How to Run

### Prerequisites

- Java 21 (JDK) installed
- Maven 3.8+ installed
- Git installed

### Steps

```bash
# 1. Clone the repository (if from GitHub)
git clone <your-repo-url>
cd certificate-verification

# 2. Build the project
mvn clean install

# 3. Run the application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

### Available Pages (Day 3)

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage |
| `http://localhost:8080/issue` | Issue Certificate form |
| `http://localhost:8080/certificates` | Certificate Management (All Certificates) |
| `http://localhost:8080/certificate/{id}` | Certificate Details page (includes Blockchain Hash) |
| `http://localhost:8080/blockchain` | Blockchain Explorer & Chain Validator |
| `http://localhost:8080/about` | About page |

### Available REST APIs (Day 3)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/certificates` | List all certificates |
| `POST` | `/api/certificates` | Issue a certificate (automatically added to blockchain) |
| `GET` | `/api/certificates/{id}` | Get certificate by Certificate ID |
| `POST` | `/api/certificates/{id}/revoke` | Revoke certificate |
| `GET` | `/api/certificates/health` | Certificate API health check |
| `GET` | `/api/blockchain` | Get all blockchain blocks |
| `POST` | `/api/blockchain/add` | Add a new block to blockchain |
| `GET` | `/api/blockchain/validate` | Validate blockchain integrity |

---

## Planned Features (Day 4+)

- [ ] Certificate verification page (online/offline)
- [ ] RSA digital signatures
- [ ] QR code generation with ZXing
- [ ] Offline verification via QR scan
- [ ] Audit trail dashboard

---

## Git Commit History

| Commit | Description |
|---|---|
| `Day 1` | Project setup and basic UI |
| `Day 2` | Certificate issuing module |
| `Day 3` | Java blockchain implementation |

---

*College Project · Java 21 · Spring Boot · Blockchain*
