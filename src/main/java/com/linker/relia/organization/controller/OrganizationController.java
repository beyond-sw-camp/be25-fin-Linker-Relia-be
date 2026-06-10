package com.linker.relia.organization.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationChartResponse>> getOrganizationChart(
            @Valid @ModelAttribute OrganizationChartRequest request) {
        OrganizationChartResponse responseDto = organizationService.getOrganizationChart(request);
        return ApiResponse.success(HttpStatus.OK, "조직도 조회 성공", responseDto);
    }
}
