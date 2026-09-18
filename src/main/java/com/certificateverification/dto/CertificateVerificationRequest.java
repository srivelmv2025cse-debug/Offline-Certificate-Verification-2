package com.certificateverification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a certificate verification request.
 * Contains the certificate data to be hashed and checked against the blockchain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateVerificationRequest {

    /** Unique certificate identifier */
    @JsonAlias({"id", "certId"})
    private String certificateId;

    /** Full student name */
    @JsonAlias({"student", "student_name"})
    private String studentName;

    /** Course or degree name */
    @JsonAlias({"course", "course_name"})
    private String courseName;

    /** Issuing institution name */
    @JsonAlias({"institution", "institution_name"})
    private String institutionName;

    /** Issue date in ISO string format (e.g., YYYY-MM-DD) */
    @JsonAlias({"date", "issue_date"})
    private String issueDate;

    /** Optional certificate type (e.g., Degree, Diploma) */
    @JsonAlias({"type", "certificate_type"})
    private String certificateType;

    /** Optional expiry date */
    @JsonAlias({"expiry", "expiry_date"})
    private String expiryDate;

    public String getStudent() {
        return studentName;
    }

    public String getCourse() {
        return courseName;
    }

    public String getInstitution() {
        return institutionName;
    }
}
