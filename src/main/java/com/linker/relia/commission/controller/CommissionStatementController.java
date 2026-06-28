package com.linker.relia.commission.controller;

import com.linker.relia.commission.service.CommissionStatementPdfService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/commissions/statements")
public class CommissionStatementController {
    private final CommissionStatementPdfService commissionStatementPdfService;

    @GetMapping("/hq/pdf")
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<byte[]> getHqCommissionStatementPdf(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam String closingMonth
    ) {
        byte[] pdf = commissionStatementPdfService.generateHqStatement(principalDetails, closingMonth);
        return pdfResponse(pdf, "commission-statement-hq-" + closingMonth + ".pdf");
    }

    @GetMapping("/organizations/{organizationCode}/pdf")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<byte[]> getBranchCommissionStatementPdf(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable String organizationCode,
            @RequestParam String closingMonth
    ) {
        byte[] pdf = commissionStatementPdfService.generateBranchStatement(
                principalDetails,
                closingMonth,
                organizationCode
        );
        return pdfResponse(pdf, "commission-statement-branch-" + organizationCode + "-" + closingMonth + ".pdf");
    }

    @GetMapping("/branch/pdf")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<byte[]> getOwnBranchCommissionStatementPdf(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam String closingMonth
    ) {
        String organizationCode = principalDetails.getUser().getOrganization().getOrganizationCode();
        byte[] pdf = commissionStatementPdfService.generateBranchStatement(
                principalDetails,
                closingMonth,
                organizationCode
        );
        return pdfResponse(pdf, "commission-statement-branch-" + organizationCode + "-" + closingMonth + ".pdf");
    }

    @GetMapping("/fps/{fpId}/pdf")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<byte[]> getFpCommissionStatementPdf(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID fpId,
            @RequestParam String closingMonth
    ) {
        byte[] pdf = commissionStatementPdfService.generateFpStatement(principalDetails, closingMonth, fpId);
        return pdfResponse(pdf, "commission-statement-fp-" + fpId + "-" + closingMonth + ".pdf");
    }

    @GetMapping("/me/pdf")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<byte[]> getMyCommissionStatementPdf(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam String closingMonth
    ) {
        UUID fpId = principalDetails.getUser().getId();
        byte[] pdf = commissionStatementPdfService.generateFpStatement(principalDetails, closingMonth, fpId);
        return pdfResponse(pdf, "commission-statement-fp-me-" + closingMonth + ".pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString()
                )
                .body(pdf);
    }
}
