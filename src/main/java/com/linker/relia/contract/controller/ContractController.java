package com.linker.relia.contract.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.contract.dto.ContractCreateRequest;
import com.linker.relia.contract.dto.ContractCreateResponse;
import com.linker.relia.contract.dto.ContractDetailResponse;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListRequest;
import com.linker.relia.contract.dto.ContractMonthlyTrendResponse;
import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.contract.service.ContractService;
import com.linker.relia.security.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")
@SecurityRequirement(name = "Bearer Authentication")
public class ContractController {
    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<ContractCreateResponse>> createContract(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody ContractCreateRequest request
    ) {
        ContractCreateResponse responseDto = contractService.createContract(principalDetails, request);
        return ApiResponse.success(HttpStatus.CREATED, "계약 등록 성공", responseDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ContractListItemResponse>>> getContracts(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute ContractListRequest request
    ) {
        PageResponse<ContractListItemResponse> responseDto = contractService.getContracts(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "보유 계약 목록 조회 성공", responseDto);
    }

    @GetMapping("/insurance-companies")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InsuranceCompanyContractStatusResponse>>> getInsuranceCompanyContractStatuses(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute ContractSummaryRequest request
    ) {
        List<InsuranceCompanyContractStatusResponse> responseDto =
                contractService.getInsuranceCompanyContractStatuses(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "보험사별 계약 현황 조회 성공", responseDto);
    }

    @GetMapping("/monthly-trend")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<ContractMonthlyTrendResponse>>> getMonthlyContractTrend(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute ContractSummaryRequest request
    ) {
        List<ContractMonthlyTrendResponse> responseDto =
                contractService.getMonthlyContractTrend(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "월별 계약 추이 조회 성공", responseDto);
    }

    @GetMapping("/{contractId}")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractDetail(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID contractId
    ) {
        ContractDetailResponse responseDto = contractService.getContractDetail(principalDetails, contractId);
        return ApiResponse.success(HttpStatus.OK, "계약 상세 조회 성공", responseDto);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ContractSummaryResponse>> getContractSummary(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute ContractSummaryRequest request
    ) {
        ContractSummaryResponse responseDto = contractService.getContractSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "보유 계약 요약 통계 조회 성공", responseDto);
    }
}
