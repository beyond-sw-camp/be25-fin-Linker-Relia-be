package com.linker.relia.commission.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.commission.dto.CommissionPaymentTypeSummaryResponse;
import com.linker.relia.commission.dto.FpCommissionListRequest;
import com.linker.relia.commission.dto.FpCommissionListResponse;
import com.linker.relia.commission.dto.FpCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendResponse;
import com.linker.relia.commission.dto.OrganizationCommissionSummaryResponse;
import com.linker.relia.commission.dto.OrganizationScopedClosingMonthRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/commissions")
public class CommissionController {
    private final CommissionService commissionService;

    @GetMapping("/fp-summary")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpCommissionSummaryResponse>> getFpCommissionSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute FpCommissionSummaryRequest request
    ) {
        FpCommissionSummaryResponse response = commissionService.getFpCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 수수료 요약 조회 성공", response);
    }

    @GetMapping("/fp-trend")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<List<FpCommissionMonthlyTrendResponse>>> getFpCommissionTrend(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        List<FpCommissionMonthlyTrendResponse> response = commissionService.getFpCommissionTrend(principalDetails);
        return ApiResponse.success(HttpStatus.OK, "설계사 월별 수수료 추이 조회 성공", response);
    }

    @GetMapping("/fps")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<FpCommissionListResponse>>> getFpCommissionList(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute FpCommissionListRequest request
    ) {
        PageResponse<FpCommissionListResponse> response =
                commissionService.getFpCommissionList(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사별 월 수수료 현황 조회 성공", response);
    }

    @GetMapping("/organization-summary")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationCommissionSummaryResponse>> getOrganizationCommissionSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute OrganizationScopedClosingMonthRequest request
    ) {
        OrganizationCommissionSummaryResponse response = commissionService.getOrganizationCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "조직 수수료 요약 조회 성공", response);
    }

    @GetMapping("/organization-trend")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<OrganizationCommissionMonthlyTrendResponse>>> getOrganizationCommissionTrend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) String organizationCode
    ) {
        List<OrganizationCommissionMonthlyTrendResponse> response =
                commissionService.getOrganizationCommissionTrend(principalDetails, organizationCode);
        return ApiResponse.success(HttpStatus.OK, "조직 월별 수수료 추이 조회 성공", response);
    }

    @GetMapping("/payment-types/summary")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CommissionPaymentTypeSummaryResponse>> getCommissionPaymentTypeSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute OrganizationScopedClosingMonthRequest request
    ) {
        CommissionPaymentTypeSummaryResponse response = commissionService.getCommissionPaymentTypeSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "지급 구분 요약 조회 성공", response);
    }

    @GetMapping("/insurance-companies/summary")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCompanyCommissionSummaryResponse>> getInsuranceCompanyCommissionSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute OrganizationScopedClosingMonthRequest request
    ) {
        InsuranceCompanyCommissionSummaryResponse response = commissionService.getInsuranceCompanyCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "보험사별 수수료 현황 조회 성공", response);
    }
}
