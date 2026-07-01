package com.linker.relia.organization.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpContractListRequest;
import com.linker.relia.organization.dto.FpContractListResponse;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.FpMonthlyPerformanceResponse;
import com.linker.relia.organization.dto.FpResignRequest;
import com.linker.relia.organization.dto.FpResignResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.organization.dto.OrganizationMemberItemResponse;
import com.linker.relia.organization.dto.OrganizationMemberListRequest;
import com.linker.relia.organization.service.OrganizationService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<BranchOrganizationResponse>>> getBranchOrganizations() {
        List<BranchOrganizationResponse> responseDto = organizationService.getBranchOrganizations();
        return ApiResponse.success(HttpStatus.OK, "지점 목록 조회 성공", responseDto);
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrganizationMemberItemResponse>>> getOrganizationMembers(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute OrganizationMemberListRequest request
    ) {
        PageResponse<OrganizationMemberItemResponse> responseDto = PageResponse.from(
                organizationService.getOrganizationMembers(principalDetails, request)
        );
        return ApiResponse.success(HttpStatus.OK, "조직 구성원 목록 조회 성공", responseDto);
    }

    @GetMapping("/fps")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<FpListResponse>> getFps(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String closingMonth,
            @RequestParam(required = false, defaultValue = "false") Boolean includeResigned
    ) {
        FpListRequest request = new FpListRequest();
        request.setPage(page);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setOrganizationId(organizationId);
        request.setClosingMonth(closingMonth);
        request.setIncludeResigned(includeResigned);

        FpListResponse responseDto = organizationService.getFps(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 목록 조회 성공", responseDto);
    }

    @GetMapping("/fps/me")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpDetailResponse>> getMyFpDetail(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) String closingMonth
    ) {
        FpDetailResponse responseDto = organizationService.getMyFpDetail(principalDetails, closingMonth);
        return ApiResponse.success(HttpStatus.OK, "내 설계사 상세 정보 조회 성공", responseDto);
    }

    @GetMapping("/fps/{fpId}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<FpDetailResponse>> getFpDetail(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID fpId,
            @RequestParam(required = false) String closingMonth
    ) {
        FpDetailResponse responseDto = organizationService.getFpDetail(principalDetails, fpId, closingMonth);
        return ApiResponse.success(HttpStatus.OK, "설계사 상세 정보 조회 성공", responseDto);
    }

    @GetMapping("/fps/me/contracts")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpContractListResponse>> getMyFpContracts(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @ModelAttribute FpContractListRequest request
    ) {
        FpContractListResponse responseDto = organizationService.getMyFpContracts(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "내 설계사 계약 목록 조회 성공", responseDto);
    }

    @GetMapping("/fps/{fpId}/contracts")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<FpContractListResponse>> getFpContracts(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID fpId,
            @Valid @ModelAttribute FpContractListRequest request
    ) {
        FpContractListResponse responseDto = organizationService.getFpContracts(principalDetails, fpId, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 계약 목록 조회 성공", responseDto);
    }

    @GetMapping("/fps/{fpId}/performance-monthly")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<FpMonthlyPerformanceResponse>> getFpMonthlyPerformances(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID fpId,
            @RequestParam(required = false) String fromClosingMonth,
            @RequestParam(required = false) String toClosingMonth
    ) {
        FpMonthlyPerformanceResponse responseDto = organizationService.getFpMonthlyPerformances(
                principalDetails,
                fpId,
                fromClosingMonth,
                toClosingMonth
        );
        return ApiResponse.success(HttpStatus.OK, "설계사 월별 성과 조회 성공", responseDto);
    }

    @PatchMapping("/fps/{fpId}/resign")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<FpResignResponse>> resignFp(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID fpId,
            @Valid @RequestBody FpResignRequest request
    ) {
        FpResignResponse responseDto = organizationService.resignFp(principalDetails, fpId, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 해촉 처리 성공", responseDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationChartResponse>> getOrganizationChart(
            @Valid @ModelAttribute OrganizationChartRequest request) {
        OrganizationChartResponse responseDto = organizationService.getOrganizationChart(request);
        return ApiResponse.success(HttpStatus.OK, "조직도 조회 성공", responseDto);
    }
}
