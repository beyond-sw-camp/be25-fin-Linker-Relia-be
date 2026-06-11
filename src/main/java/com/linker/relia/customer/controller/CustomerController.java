package com.linker.relia.customer.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.consultation.dto.request.ConsultationHistoryRequest;
import com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryItemResponse;
import com.linker.relia.customer.dto.CustomerFpHistoryRequest;
import com.linker.relia.customer.dto.CustomerInterestListRequest;
import com.linker.relia.customer.dto.CustomerInterestListResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import com.linker.relia.customer.service.CustomerService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CustomerListResponse>> getCustomers(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                          @Valid @ModelAttribute CustomerListRequest request) {
        CustomerListResponse responseDto = customerService.getCustomers(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "고객 목록 조회 성공", responseDto);
    }

    @GetMapping("/interests")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CustomerInterestListResponse>> getInterestCustomers(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                          @Valid @ModelAttribute CustomerInterestListRequest request) {
        CustomerInterestListResponse responseDto = customerService.getInterestCustomers(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "관심 고객 목록 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                 @PathVariable UUID customerId) {
        CustomerDetailResponse responseDto = customerService.getCustomerDetail(principalDetails, customerId);
        return ApiResponse.success(HttpStatus.OK, "고객 상세 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}/contracts")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerOwnedContractResponse>>> getOwnCustomerContracts(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                                    @PathVariable UUID customerId) {
        List<CustomerOwnedContractResponse> responseDto = customerService.getOwnCustomerContracts(principalDetails, customerId);
        return ApiResponse.success(HttpStatus.OK, "고객 보유 계약 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}/consultations")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ConsultationHistoryItemResponse>>> getOwnCustomerConsultations(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                                                   @PathVariable UUID customerId,
                                                                                                                   @Valid @ModelAttribute ConsultationHistoryRequest request) {
        PageResponse<ConsultationHistoryItemResponse> responseDto = customerService.getOwnCustomerConsultations(
                principalDetails,
                customerId,
                request
        );
        return ApiResponse.success(HttpStatus.OK, "고객 상담 이력 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}/fp-histories")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerFpHistoryItemResponse>>> getCustomerFpHistories(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                                           @PathVariable UUID customerId,
                                                                                                           @Valid @ModelAttribute CustomerFpHistoryRequest request) {
        PageResponse<CustomerFpHistoryItemResponse> responseDto = customerService.getCustomerFpHistories(
                principalDetails,
                customerId,
                request
        );
        return ApiResponse.success(HttpStatus.OK, "담당 설계사 변경 이력 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}/ai-briefing")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<CustomerAiBriefingResponse>> getCustomerAiBriefing(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                         @PathVariable UUID customerId) {
        CustomerAiBriefingResponse responseDto = customerService.getCustomerAiBriefing(principalDetails, customerId);
        return ApiResponse.success(HttpStatus.OK, "고객 AI 상담 브리핑 조회 성공", responseDto);
    }
}
