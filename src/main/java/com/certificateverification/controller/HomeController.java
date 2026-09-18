package com.certificateverification.controller;

import com.certificateverification.blockchain.Blockchain;
import com.certificateverification.dto.CertificateVerificationRequest;
import com.certificateverification.dto.CertificateVerificationResponse;
import com.certificateverification.model.Certificate;
import com.certificateverification.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Web MVC controller serving UI pages: home, issue, management, blockchain explorer, and verification.
 * Day 4: Added Verify Certificate UI page (/verify).
 */
@Controller
public class HomeController {

    private final CertificateService certificateService;
    private final Blockchain blockchain;

    @Autowired
    public HomeController(CertificateService certificateService,
                          Blockchain blockchain) {
        this.certificateService = certificateService;
        this.blockchain = blockchain;
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
            model.addAttribute("pageTitle", "Certificate Details - " + id);
            model.addAttribute("certificate", certOpt.get());
            return "certificate-details";
        } else {
            model.addAttribute("pageTitle", "Certificate Not Found");
            model.addAttribute("errorMessage", "Certificate with ID " + id + " not found.");
            return "error";
        }
    }

    @GetMapping("/certificates")
    public String listCertificates(Model model) {
        model.addAttribute("pageTitle", "Certificate Management");
        model.addAttribute("certificates", certificateService.getAllCertificates());
        return "certificates";
    }

    @PostMapping("/certificates/{id}/revoke")
    public String revokeCertificate(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            certificateService.revokeCertificate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Certificate " + id + " was revoked successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/certificates";
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
}
