package com.linker.relia.dashboard.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.dashboard.dto.DashboardBranchRankingRequest;
import com.linker.relia.dashboard.dto.DashboardBranchRankingResponse;
import com.linker.relia.dashboard.dto.DashboardClosingMonthOptionResponse;
import com.linker.relia.dashboard.dto.DashboardFpRankingRequest;
import com.linker.relia.dashboard.dto.DashboardFpRankingResponse;
import com.linker.relia.dashboard.dto.DashboardKpiRequest;
import com.linker.relia.dashboard.dto.DashboardOrganizationContractDistributionRequest;
import com.linker.relia.dashboard.dto.FpDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.FpDashboardContractStatusResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyCommissionTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardMonthlyContractCustomerTrendResponse;
import com.linker.relia.dashboard.dto.FpDashboardSummaryResponse;
import com.linker.relia.dashboard.dto.OrganizationDashboardContractDistributionResponse;
import com.linker.relia.dashboard.dto.OrganizationDashboardKpiResponse;
import com.linker.relia.dashboard.service.DashboardService;
import com.linker.relia.security.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/organization/summary")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationDashboardKpiResponse>> getOrganizationDashboardSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute DashboardKpiRequest request
    ) {
        OrganizationDashboardKpiResponse response = dashboardService.getOrganizationKpi(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "본사/지점 대시보드 KPI 요약 조회를 성공하였습니다.", response);
    }

    @GetMapping("/organization/contracts/distribution")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationDashboardContractDistributionResponse>>
    getOrganizationDashboardContractDistribution(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute DashboardOrganizationContractDistributionRequest request
    ) {
        OrganizationDashboardContractDistributionResponse response =
                dashboardService.getOrganizationContractDistribution(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "본사/지점 대시보드 계약 분포 조회를 성공하였습니다.", response);
    }

    @GetMapping("/organization/rankings/fps")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardFpRankingResponse>> getOrganizationFpRankings(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute DashboardFpRankingRequest request
    ) {
        DashboardFpRankingResponse response =
                dashboardService.getOrganizationFpRankings(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 실적 순위 조회에 성공했습니다.", response);
    }

    @GetMapping("/organization/rankings/branches")
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<DashboardBranchRankingResponse>> getOrganizationBranchRankings(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute DashboardBranchRankingRequest request
    ) {
        DashboardBranchRankingResponse response =
                dashboardService.getOrganizationBranchRankings(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "지점 수입수수료 순위 조회에 성공했습니다.", response);
    }

    @GetMapping("/filters/closing-months")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<DashboardClosingMonthOptionResponse>>> getDashboardClosingMonthOptions() {
        List<DashboardClosingMonthOptionResponse> response = dashboardService.getClosingMonthOptions();
        return ApiResponse.success(HttpStatus.OK, "대시보드 마감월 목록 조회를 성공하였습니다.", response);
    }

    @GetMapping("/fp/summary")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDashboardSummaryResponse>> getFpDashboardSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        FpDashboardSummaryResponse response = dashboardService.getFpSummary(principalDetails, referenceDate);
        return ApiResponse.success(HttpStatus.OK, "설계사 대시보드 요약 조회를 성공하였습니다.", response);
    }

    @GetMapping("/fp/contracts/status")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDashboardContractStatusResponse>> getFpDashboardContractStatus(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        FpDashboardContractStatusResponse response =
                dashboardService.getFpContractStatus(principalDetails, referenceDate);
        return ApiResponse.success(HttpStatus.OK, "설계사 대시보드 계약 상태 조회를 성공하였습니다.", response);
    }

    @GetMapping("/fp/contracts/distribution")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDashboardContractDistributionResponse>> getFpDashboardContractDistribution(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        FpDashboardContractDistributionResponse response =
                dashboardService.getFpContractDistribution(principalDetails, referenceDate);
        return ApiResponse.success(HttpStatus.OK, "설계사 대시보드 계약 분포 조회를 성공하였습니다.", response);
    }

    @GetMapping("/fp/monthly-contract-customer-trend")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDashboardMonthlyContractCustomerTrendResponse>>
    getFpDashboardMonthlyContractCustomerTrend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        FpDashboardMonthlyContractCustomerTrendResponse response =
                dashboardService.getFpMonthlyContractCustomerTrend(principalDetails, referenceDate);
        return ApiResponse.success(HttpStatus.OK, "설계사 대시보드 월별 계약/고객 추이 조회를 성공하였습니다.", response);
    }

    @GetMapping("/fp/monthly-commission-trend")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDashboardMonthlyCommissionTrendResponse>>
    getFpDashboardMonthlyCommissionTrend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(value = "referenceDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate
    ) {
        FpDashboardMonthlyCommissionTrendResponse response =
                dashboardService.getFpMonthlyCommissionTrend(principalDetails, referenceDate);
        return ApiResponse.success(HttpStatus.OK, "설계사 대시보드 월별 수수료 추이 조회를 성공하였습니다.", response);
    }
}
