package com.certificateverification.controller;

import com.certificateverification.model.Certificate;
import com.certificateverification.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Home controller serving the main landing page.
 * Day 1: Returns the homepage view.
 */
@Controller
public class HomeController {

    private final CertificateService certificateService;

    @Autowired
    public HomeController(CertificateService certificateService) {
        this.certificateService = certificateService;
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
}
