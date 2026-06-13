package com.linker.relia.organization.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.FpListRequest;
import com.linker.relia.organization.dto.FpListResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
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

    /**
     * Retrieve the list of branch organizations.
     *
     * @return ResponseEntity containing an ApiResponse with HTTP status 200, message "지점 목록 조회 성공",
     *         and the list of BranchOrganizationResponse objects.
     */
    @GetMapping("/branches")
    public ResponseEntity<ApiResponse<List<BranchOrganizationResponse>>> getBranchOrganizations() {
        List<BranchOrganizationResponse> responseDto = organizationService.getBranchOrganizations();
        return ApiResponse.success(HttpStatus.OK, "지점 목록 조회 성공", responseDto);
    }

    /**
     * Retrieve a paginated list of financial planners (FPs) filtered by optional criteria.
     *
     * @param principalDetails the authenticated principal initiating the request
     * @param page              zero-based page index to return
     * @param size              the number of items per page
     * @param keyword           optional search keyword to filter FP names or metadata
     * @param organizationId    optional organization UUID to restrict results to a specific organization
     * @param closingMonth      optional closing month filter in string form
     * @return                  an ApiResponse wrapping an FpListResponse with the matching FPs and pagination information
     */
    @GetMapping("/fps")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<FpListResponse>> getFps(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam Integer page,
            @RequestParam Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String closingMonth
    ) {
        FpListRequest request = new FpListRequest();
        request.setPage(page);
        request.setSize(size);
        request.setKeyword(keyword);
        request.setOrganizationId(organizationId);
        request.setClosingMonth(closingMonth);

        FpListResponse responseDto = organizationService.getFps(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 목록 조회 성공", responseDto);
    }

    /**
     * Retrieve the organization chart based on the provided request parameters.
     *
     * @param request validated OrganizationChartRequest containing filters and options for the chart
     * @return a ResponseEntity containing an ApiResponse with the OrganizationChartResponse and HTTP 200 status
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationChartResponse>> getOrganizationChart(
            @Valid @ModelAttribute OrganizationChartRequest request) {
        OrganizationChartResponse responseDto = organizationService.getOrganizationChart(request);
        return ApiResponse.success(HttpStatus.OK, "조직도 조회 성공", responseDto);
    }
}
