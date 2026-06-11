package com.linker.relia.insurance.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.insurance.dto.InsuranceCategoryResponse;
import com.linker.relia.insurance.dto.InsuranceCompanyResponse;
import com.linker.relia.insurance.dto.InsuranceProductListRequest;
import com.linker.relia.insurance.dto.InsuranceProductResponse;
import com.linker.relia.insurance.service.InsuranceService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance")
@SecurityRequirement(name = "Bearer Authentication")
public class InsuranceController {
    private final InsuranceService insuranceService;

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InsuranceCompanyResponse>>> getInsuranceCompanies() {
        List<InsuranceCompanyResponse> responseDto = insuranceService.getInsuranceCompanies();
        return ApiResponse.success(HttpStatus.OK, "보험사 목록 조회 성공", responseDto);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InsuranceCategoryResponse>>> getInsuranceCategories() {
        List<InsuranceCategoryResponse> responseDto = insuranceService.getInsuranceCategories();
        return ApiResponse.success(HttpStatus.OK, "보종 목록 조회 성공", responseDto);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InsuranceProductResponse>>> getInsuranceProducts(
            @Valid @ModelAttribute InsuranceProductListRequest request
    ) {
        List<InsuranceProductResponse> responseDto = insuranceService.getInsuranceProducts(request);
        return ApiResponse.success(HttpStatus.OK, "보험상품 목록 조회 성공", responseDto);
    }
}
