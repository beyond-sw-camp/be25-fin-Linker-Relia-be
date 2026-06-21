package com.linker.relia.insurance.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.service.InsuranceManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/insurance-management")
@SecurityRequirement(name = "Bearer Authentication")
public class InsuranceManagementController {
    private final InsuranceManagementService insuranceManagementService;

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InsuranceManagementCompanyListItemResponse>>> getInsuranceCompanies(
            @Valid @ModelAttribute InsuranceManagementCompanyListRequest request
    ) {
        PageResponse<InsuranceManagementCompanyListItemResponse> response =
                insuranceManagementService.getInsuranceCompanies(request);
        return ApiResponse.success(HttpStatus.OK, "보험사 목록 조회 성공", response);
    }
}
