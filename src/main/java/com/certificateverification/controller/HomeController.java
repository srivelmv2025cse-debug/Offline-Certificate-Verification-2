package com.certificateverification.controller;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.Certificate;
import com.certificateverification.qr.QRService;
import com.certificateverification.qr.QRVerificationRequest;
import com.certificateverification.qr.QRVerificationResponse;
import com.certificateverification.service.CertificateService;
import com.certificateverification.offline.OfflineBlockchainService;
import com.certificateverification.offline.OfflineSyncPackage;
import com.certificateverification.offline.OfflineVerificationResult;
import com.certificateverification.offline.OfflineVerificationService;
import com.certificateverification.offline.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Web MVC controller serving UI pages: home, issue, management, blockchain explorer,
 * canonical verification, Day 5 QR code verification, and Day 8 Offline verification.
 */
@Controller
public class HomeController {

    private final CertificateService certificateService;
    private final Blockchain blockchain;
    private final QRService qrService;
    private final com.certificateverification.service.AuditLogService auditLogService;
    private final OfflineVerificationService offlineVerificationService;
    private final SyncService syncService;
    private final OfflineBlockchainService offlineBlockchainService;

    public HomeController(CertificateService certificateService,
                          Blockchain blockchain,
                          QRService qrService) {
        this(certificateService, blockchain, qrService, null, null, null, null);
    }

    @Autowired
    public HomeController(CertificateService certificateService,
                          Blockchain blockchain,
                          QRService qrService,
                          @Autowired(required = false) com.certificateverification.service.AuditLogService auditLogService,
                          @Autowired(required = false) OfflineVerificationService offlineVerificationService,
                          @Autowired(required = false) SyncService syncService,
                          @Autowired(required = false) OfflineBlockchainService offlineBlockchainService) {
        this.certificateService = certificateService;
        this.blockchain = blockchain;
        this.qrService = qrService;
        this.auditLogService = auditLogService;
        this.offlineVerificationService = offlineVerificationService;
        this.syncService = syncService;
        this.offlineBlockchainService = offlineBlockchainService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Offline Certificate Verification Using Blockchain");
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About - Certificate Verification");
        return "about";
    }

    @GetMapping("/issue")
    public String showIssueForm(Model model) {
        model.addAttribute("pageTitle", "Issue Certificate");
        model.addAttribute("certificate", new Certificate());
        return "issue";
    }

    @PostMapping("/issue")
    public String issueCertificate(@ModelAttribute Certificate certificate, RedirectAttributes redirectAttributes, Model model) {
        try {
            Certificate issued = certificateService.issueCertificate(certificate);
            redirectAttributes.addFlashAttribute("successMessage", "Certificate issued successfully.");
            return "redirect:/certificate/" + issued.getCertificateId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("pageTitle", "Issue Certificate");
            model.addAttribute("errorMessage", e.getMessage());
            return "issue";
        }
    }

    @GetMapping("/certificate/{id}")
    public String showCertificateDetails(@PathVariable("id") String id, Model model) {
        Optional<Certificate> certOpt = certificateService.getCertificateById(id);
        if (certOpt.isPresent()) {
            Certificate cert = certOpt.get();
            // Ensure QR code is generated if missing
            if (cert.getQrCodeData() == null || cert.getQrCodeData().isEmpty()) {
                try {
                    certificateService.generateQRCodeForCertificate(cert.getCertificateId());
                    cert = certificateService.getCertificateById(id).orElse(cert);
                } catch (Exception ignored) {}
            }
            model.addAttribute("pageTitle", "Certificate Details - " + id);
            model.addAttribute("certificate", cert);
            return "certificate-details";
        } else {
            model.addAttribute("pageTitle", "Certificate Not Found");
            model.addAttribute("errorMessage", "Certificate with ID " + id + " not found.");
            return "error";
        }
    }

    @PostMapping("/certificates/{id}/generate-qr")
    public String generateQRCode(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            certificateService.generateQRCodeForCertificate(id);
            redirectAttributes.addFlashAttribute("successMessage", "QR code generated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate QR code: " + e.getMessage());
        }
        return "redirect:/certificate/" + id;
    }

    @GetMapping("/certificates")
    public String listCertificates(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("pageTitle", "Certificate Management");
        if (search != null && !search.trim().isEmpty()) {
            model.addAttribute("certificates", certificateService.searchCertificates(search.trim()));
            model.addAttribute("searchQuery", search.trim());
        } else {
            model.addAttribute("certificates", certificateService.getAllCertificates());
        }
        return "certificates";
    }

    @PostMapping("/certificates/{id}/revoke")
    public String revokeCertificate(
            @PathVariable("id") String id,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            certificateService.revokeCertificate(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Certificate " + id + " was revoked successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/certificates";
    }

    @GetMapping("/revoke")
    public String showRevokePage(
            @RequestParam(value = "certificateId", required = false) String certificateId,
            Model model) {
        model.addAttribute("pageTitle", "Revoke Certificate");
        if (certificateId != null && !certificateId.trim().isEmpty()) {
            Optional<Certificate> certOpt = certificateService.getCertificateById(certificateId.trim());
            if (certOpt.isPresent()) {
                model.addAttribute("foundCertificate", certOpt.get());
            } else {
                model.addAttribute("notFoundMessage", "No certificate found with ID '" + certificateId.trim() + "'.");
            }
            model.addAttribute("searchId", certificateId.trim());
        }
        return "revoke";
    }

    @PostMapping("/revoke")
    public String handleRevokeForm(
            @RequestParam("certificateId") String certificateId,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            Certificate revoked = certificateService.revokeCertificate(certificateId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Certificate " + revoked.getCertificateId() + " has been revoked.");
            return "redirect:/certificate/" + revoked.getCertificateId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Revocation failed: " + e.getMessage());
            return "redirect:/revoke?certificateId=" + certificateId;
        }
    }

    @GetMapping("/audit")
    public String showAuditTrail(
            @RequestParam(value = "certificateId", required = false) String certificateId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "search", required = false) String search,
            Model model) {
        model.addAttribute("pageTitle", "Audit Trail");
        java.util.List<com.certificateverification.model.AuditLog> logs;
        if (search != null && !search.trim().isEmpty()) {
            logs = auditLogService != null ? auditLogService.searchLogs(search.trim()) : java.util.List.of();
            model.addAttribute("searchQuery", search.trim());
        } else if ((certificateId != null && !certificateId.trim().isEmpty()) || (action != null && !action.trim().isEmpty())) {
            logs = auditLogService != null ? auditLogService.getLogsByCertificateIdAndAction(certificateId, action) : java.util.List.of();
            model.addAttribute("filterCertId", certificateId);
            model.addAttribute("filterAction", action);
        } else {
            logs = auditLogService != null ? auditLogService.getAllLogs() : java.util.List.of();
        }
        model.addAttribute("auditLogs", logs);
        model.addAttribute("totalLogs", logs != null ? logs.size() : 0);
        return "audit";
    }

    @GetMapping("/blockchain")
    public String showBlockchain(@RequestParam(value = "validate", required = false) Boolean validate, Model model) {
        model.addAttribute("pageTitle", "Blockchain Explorer");
        model.addAttribute("blocks", blockchain.getChain());
        model.addAttribute("totalBlocks", blockchain.getChainSize());
        if (Boolean.TRUE.equals(validate)) {
            boolean isValid = blockchain.isChainValid();
            model.addAttribute("validationPerformed", true);
            model.addAttribute("isValid", isValid);
            model.addAttribute("validationMessage", isValid ? "Blockchain Valid" : "Blockchain Tampered");
            if (auditLogService != null) {
                auditLogService.log(
                        "BLOCKCHAIN_LEDGER",
                        "BLOCKCHAIN_VALIDATION",
                        isValid ? "VALID" : "TAMPERED",
                        "UI blockchain integrity check. Valid: " + isValid + ", Total blocks: " + blockchain.getChainSize(),
                        "127.0.0.1"
                );
            }
        }
        return "blockchain";
    }

    @GetMapping("/verify")
    public String showVerifyForm(@RequestParam(value = "certificateId", required = false) String certificateId, Model model) {
        model.addAttribute("pageTitle", "Verify Certificate");
        CertificateVerificationRequest req = new CertificateVerificationRequest();
        if (certificateId != null && !certificateId.trim().isEmpty()) {
            req.setCertificateId(certificateId.trim());
            certificateService.getCertificateById(certificateId.trim()).ifPresent(cert -> {
                req.setStudentName(cert.getStudentName());
                req.setCourseName(cert.getCourseName());
                req.setInstitutionName(cert.getInstitutionName());
                req.setCertificateType(cert.getCertificateType());
                req.setIssueDate(cert.getIssueDate() != null ? cert.getIssueDate().toString() : "");
                if (cert.getExpiryDate() != null) {
                    req.setExpiryDate(cert.getExpiryDate().toString());
                }
            });
        }
        model.addAttribute("verificationRequest", req);
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyCertificate(
            @ModelAttribute("verificationRequest") CertificateVerificationRequest verificationRequest,
            HttpServletRequest httpRequest,
            Model model) {
        model.addAttribute("pageTitle", "Certificate Verification Result");
        try {
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
            CertificateVerificationResponse response = certificateService.verifyCertificate(verificationRequest, ipAddress);
            model.addAttribute("response", response);
            model.addAttribute("verificationPerformed", true);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred during verification: " + e.getMessage());
        }
        return "verify";
    }

    @GetMapping("/verify-qr")
    public String showQRVerifyPage(@RequestParam(value = "certificateId", required = false) String certificateId, Model model) {
        model.addAttribute("pageTitle", "QR Code Certificate Verification");
        QRVerificationRequest req = new QRVerificationRequest();
        if (certificateId != null && !certificateId.trim().isEmpty()) {
            req.setCertificateId(certificateId.trim());
        }
        model.addAttribute("qrRequest", req);
        return "qr-verify";
    }

    @PostMapping("/verify-qr")
    public String verifyQRCode(
            @ModelAttribute("qrRequest") QRVerificationRequest qrRequest,
            HttpServletRequest httpRequest,
            Model model) {
        model.addAttribute("pageTitle", "QR Verification Result");
        try {
            String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
            QRVerificationResponse response = qrService.verifyQRCode(qrRequest, ipAddress);
            model.addAttribute("qrResponse", response);
            model.addAttribute("verificationPerformed", true);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "QR verification error: " + e.getMessage());
        }
        return "qr-verify";
    }

    // =========================================================================
    // Day 8: Offline Verification & Blockchain Synchronization
    // =========================================================================

    @GetMapping("/offline-verify")
    public String showOfflineVerifyPage(@RequestParam(value = "certificateId", required = false) String certificateId, Model model) {
        model.addAttribute("pageTitle", "Offline Certificate Verification");
        if (certificateId != null && !certificateId.trim().isEmpty()) {
            model.addAttribute("certificateId", certificateId.trim());
        }
        boolean isLoaded = syncService != null && (syncService.getLastSyncPackage() != null || syncService.isSyncFileExists());
        model.addAttribute("isLoaded", isLoaded);
        model.addAttribute("lastSyncTime", syncService != null ? syncService.getLastSyncTime() : null);
        model.addAttribute("blockCount", syncService != null ? syncService.getBlockCount() : 0);
        model.addAttribute("certCount", syncService != null ? syncService.getCertificateCount() : 0);
        return "offline-verify";
    }

    @PostMapping("/offline-verify")
    public String verifyOffline(
            @RequestParam("certificateId") String certificateId,
            @RequestParam(value = "studentName", required = false) String studentName,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "institutionName", required = false) String institutionName,
            Model model) {
        model.addAttribute("pageTitle", "Offline Verification Result");
        try {
            if (offlineVerificationService != null) {
                OfflineVerificationResult result = offlineVerificationService.verifyOffline(
                        certificateId, studentName, courseName, institutionName, null, null, null);
                model.addAttribute("result", result);
                model.addAttribute("verificationPerformed", true);
                model.addAttribute("certificateId", certificateId);
                model.addAttribute("studentName", studentName);
                model.addAttribute("courseName", courseName);
                model.addAttribute("institutionName", institutionName);

                if (auditLogService != null) {
                    auditLogService.log(
                            certificateId,
                            "OFFLINE_VERIFICATION",
                            result.getResult(),
                            "Offline verification result: " + result.getResult() + " | " + result.getMessage(),
                            "127.0.0.1-offline"
                    );
                }
            } else {
                model.addAttribute("errorMessage", "Offline verification service is not available.");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Offline verification failed: " + e.getMessage());
        }

        boolean isLoaded = syncService != null && (syncService.getLastSyncPackage() != null || syncService.isSyncFileExists());
        model.addAttribute("isLoaded", isLoaded);
        model.addAttribute("lastSyncTime", syncService != null ? syncService.getLastSyncTime() : null);
        model.addAttribute("blockCount", syncService != null ? syncService.getBlockCount() : 0);
        model.addAttribute("certCount", syncService != null ? syncService.getCertificateCount() : 0);
        return "offline-verify";
    }

    @GetMapping("/sync-status")
    public String showSyncStatus(@RequestParam(value = "validateChain", required = false) Boolean validateChain, Model model) {
        model.addAttribute("pageTitle", "Blockchain Synchronization Status");
        if (syncService != null) {
            model.addAttribute("lastSyncTime", syncService.getLastSyncTime());
            model.addAttribute("syncFileExists", syncService.isSyncFileExists());
            model.addAttribute("syncFilePath", syncService.getSyncFilePath());
            model.addAttribute("blockCount", syncService.getBlockCount());
            model.addAttribute("certificateCount", syncService.getCertificateCount());
            model.addAttribute("revokedCount", syncService.getRevokedCount());
            model.addAttribute("syncPackage", syncService.getLastSyncPackage());
            model.addAttribute("isLoaded", syncService.getLastSyncPackage() != null);
        }

        if (Boolean.TRUE.equals(validateChain) && offlineBlockchainService != null) {
            boolean chainValid = offlineBlockchainService.isChainValid();
            model.addAttribute("chainValidationPerformed", true);
            model.addAttribute("chainValid", chainValid);
            model.addAttribute("chainValidationMessage", chainValid
                    ? "Offline blockchain chain integrity is valid and cryptographically verified!"
                    : "Offline blockchain chain is invalid or tampered!");

            if (auditLogService != null) {
                auditLogService.log(
                        "OFFLINE_BLOCKCHAIN",
                        "BLOCKCHAIN_VALIDATION",
                        chainValid ? "VALID" : "TAMPERED",
                        "Offline blockchain integrity check. Valid: " + chainValid
                                + ", Total offline blocks: " + offlineBlockchainService.getChainSize(),
                        "127.0.0.1-offline"
                );
            }
        }
        return "sync-status";
    }

    @PostMapping("/sync/generate")
    public String generateSyncPackage(RedirectAttributes redirectAttributes) {
        try {
            if (syncService != null) {
                OfflineSyncPackage pkg = syncService.generateSyncPackage();
                redirectAttributes.addFlashAttribute("successMessage",
                        "Offline sync package successfully generated! Exported "
                                + pkg.getTotalBlocks() + " blocks and "
                                + pkg.getTotalCertificates() + " certificates ("
                                + pkg.getTotalRevokedCertificates() + " revoked).");

                if (auditLogService != null) {
                    auditLogService.log(
                            "SYNC_PACKAGE",
                            "SYNC_GENERATE",
                            "SUCCESS",
                            "Generated sync package with " + pkg.getTotalBlocks() + " blocks and " + pkg.getTotalCertificates() + " certs",
                            "127.0.0.1"
                    );
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "SyncService not available.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate sync package: " + e.getMessage());
        }
        return "redirect:/sync-status";
    }

    @PostMapping("/sync/import")
    public String importSyncPackage(RedirectAttributes redirectAttributes) {
        try {
            if (syncService != null) {
                OfflineSyncPackage pkg = syncService.importDefaultSyncPackage();
                redirectAttributes.addFlashAttribute("successMessage",
                        "Offline sync package imported successfully! Loaded "
                                + pkg.getTotalBlocks() + " blocks and "
                                + pkg.getTotalCertificates() + " certificates.");

                if (auditLogService != null) {
                    auditLogService.log(
                            "SYNC_PACKAGE",
                            "SYNC_IMPORT",
                            "SUCCESS",
                            "Imported sync package with " + pkg.getTotalBlocks() + " blocks and " + pkg.getTotalCertificates() + " certs",
                            "127.0.0.1"
                    );
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "SyncService not available.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to import sync package: " + e.getMessage());
        }
        return "redirect:/sync-status";
    }
}
