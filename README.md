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

### Available Pages (Day 1)

| URL | Description |
|---|---|
| `http://localhost:8080/` | Homepage |
| `http://localhost:8080/about` | About page |
| `http://localhost:8080/api/certificates/health` | API health check (JSON) |

---

## Planned Features (Day 2+)

- [ ] SHA-256 certificate hashing
- [ ] Local blockchain with proof-of-work
- [ ] Certificate issuance form
- [ ] Certificate verification page
- [ ] RSA digital signatures
- [ ] QR code generation with ZXing
- [ ] Offline verification via QR scan
- [ ] Certificate revocation
- [ ] Audit trail dashboard
- [ ] Blockchain integrity checker

---

## Git Commit History

| Commit | Description |
|---|---|
| `Day 1` | Project setup and basic UI |

---

*College Project · Java 21 · Spring Boot · Blockchain*
