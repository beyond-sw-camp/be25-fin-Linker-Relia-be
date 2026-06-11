package com.linker.relia.insurance.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.insurance.dto.request.InsuranceCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyPageResponse;
import com.linker.relia.insurance.service.InsuranceCompanyService;
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
@RequestMapping("/api/insurance-companies")
@SecurityRequirement(name = "Bearer Authentication")
public class InsuranceCompanyController {
    private final InsuranceCompanyService insuranceCompanyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCompanyPageResponse>> getPartnerInsuranceCompanies(
            @Valid @ModelAttribute InsuranceCompanyListRequest request
    ) {
        InsuranceCompanyPageResponse response = insuranceCompanyService.getPartnerInsuranceCompanies(request);
        return ApiResponse.success(HttpStatus.OK, "제휴 보험사 목록 조회 성공", response);
    }
}
