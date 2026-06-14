package com.linker.relia.commission.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryRequest;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryRequest;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.service.CommissionService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/commissions")
public class CommissionController {
    private final CommissionService commissionService;

    @GetMapping("/fp-summary")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpCommissionSummaryResponse>> getFpCommissionSummary(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                           @Valid @ModelAttribute FpCommissionSummaryRequest request) {
        FpCommissionSummaryResponse response = commissionService.getFpCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 수수료 요약 조회를 성공하였습니다.", response);
    }

    @GetMapping("/organization-summary")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationCommissionSummaryResponse>> getOrganizationCommissionSummary(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                                               @Valid @ModelAttribute OrganizationCommissionSummaryRequest request) {
        OrganizationCommissionSummaryResponse response = commissionService.getOrganizationCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "조직 수수료 요약 조회를 성공하였습니다.", response);
    }

    @GetMapping("/payment-types/summary")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CommissionPaymentTypeSummaryResponse>> getCommissionPaymentTypeSummary(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                                             @Valid @ModelAttribute CommissionPaymentTypeSummaryRequest request) {
        CommissionPaymentTypeSummaryResponse response = commissionService.getCommissionPaymentTypeSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "지급 구분 요약 조회를 성공하였습니다.", response);
    }
}
