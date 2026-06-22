package com.linker.relia.insurance.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.insurance.dto.InsuranceCategoryResponse;
import com.linker.relia.insurance.dto.request.InsuranceCategoryCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCategoryUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementProductListRequest;
import com.linker.relia.insurance.dto.request.InsuranceProductCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceProductUpdateRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyCreateResponse;
import com.linker.relia.insurance.dto.response.InsuranceCompanyDetailResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCategoryResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementProductListItemResponse;
import com.linker.relia.insurance.dto.response.InsuranceProductDetailResponse;
import com.linker.relia.insurance.service.InsuranceManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance-management")
@SecurityRequirement(name = "Bearer Authentication")
public class InsuranceManagementController {
    private final InsuranceManagementService insuranceManagementService;

    @PostMapping("/categories")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCategoryResponse>> createInsuranceCategory(
            @Valid @RequestBody InsuranceCategoryCreateRequest request
    ) {
        InsuranceCategoryResponse response = insuranceManagementService.createInsuranceCategory(request);
        return ApiResponse.success(HttpStatus.CREATED, "보종 등록 성공", response);
    }

    @PatchMapping("/categories/{insuranceCategoryId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCategoryResponse>> updateInsuranceCategory(
            @PathVariable UUID insuranceCategoryId,
            @Valid @RequestBody InsuranceCategoryUpdateRequest request
    ) {
        InsuranceCategoryResponse response =
                insuranceManagementService.updateInsuranceCategory(insuranceCategoryId, request);
        return ApiResponse.success(HttpStatus.OK, "보종 수정 성공", response);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<InsuranceManagementCategoryResponse>>> getInsuranceCategories() {
        List<InsuranceManagementCategoryResponse> response = insuranceManagementService.getInsuranceCategories();
        return ApiResponse.success(HttpStatus.OK, "보종 목록 조회 성공", response);
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InsuranceManagementCompanyListItemResponse>>> getInsuranceCompanies(
            @Valid @ModelAttribute InsuranceManagementCompanyListRequest request
    ) {
        PageResponse<InsuranceManagementCompanyListItemResponse> response =
                insuranceManagementService.getInsuranceCompanies(request);
        return ApiResponse.success(HttpStatus.OK, "보험사 목록 조회 성공", response);
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InsuranceManagementProductListItemResponse>>> getInsuranceProducts(
            @Valid @ModelAttribute InsuranceManagementProductListRequest request
    ) {
        PageResponse<InsuranceManagementProductListItemResponse> response =
                insuranceManagementService.getInsuranceProducts(request);
        return ApiResponse.success(HttpStatus.OK, "보험상품 목록 조회 성공", response);
    }

    @GetMapping("/products/{insuranceProductId}")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceProductDetailResponse>> getInsuranceProductDetail(
            @PathVariable UUID insuranceProductId
    ) {
        InsuranceProductDetailResponse response =
                insuranceManagementService.getInsuranceProductDetail(insuranceProductId);
        return ApiResponse.success(HttpStatus.OK, "보험상품 상세 조회 성공", response);
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceProductDetailResponse>> createInsuranceProduct(
            @Valid @RequestBody InsuranceProductCreateRequest request
    ) {
        InsuranceProductDetailResponse response = insuranceManagementService.createInsuranceProduct(request);
        return ApiResponse.success(HttpStatus.CREATED, "보험상품 등록 성공", response);
    }

    @PatchMapping("/products/{insuranceProductId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceProductDetailResponse>> updateInsuranceProduct(
            @PathVariable UUID insuranceProductId,
            @Valid @RequestBody InsuranceProductUpdateRequest request
    ) {
        InsuranceProductDetailResponse response =
                insuranceManagementService.updateInsuranceProduct(insuranceProductId, request);
        return ApiResponse.success(HttpStatus.OK, "보험상품 수정 성공", response);
    }

    @GetMapping("/companies/{insuranceCompanyId}")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCompanyDetailResponse>> getInsuranceCompanyDetail(
            @PathVariable UUID insuranceCompanyId
    ) {
        InsuranceCompanyDetailResponse response =
                insuranceManagementService.getInsuranceCompanyDetail(insuranceCompanyId);
        return ApiResponse.success(HttpStatus.OK, "보험사 상세 조회 성공", response);
    }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCompanyCreateResponse>> createInsuranceCompany(
            @Valid @RequestBody InsuranceCompanyCreateRequest request
    ) {
        InsuranceCompanyCreateResponse response = insuranceManagementService.createInsuranceCompany(request);
        return ApiResponse.success(HttpStatus.CREATED, "보험사 등록 성공", response);
    }

    @PatchMapping("/companies/{insuranceCompanyId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<InsuranceCompanyDetailResponse>> updateInsuranceCompany(
            @PathVariable UUID insuranceCompanyId,
            @Valid @RequestBody InsuranceCompanyUpdateRequest request
    ) {
        InsuranceCompanyDetailResponse response =
                insuranceManagementService.updateInsuranceCompany(insuranceCompanyId, request);
        return ApiResponse.success(HttpStatus.OK, "보험사 수정 성공", response);
    }
}
