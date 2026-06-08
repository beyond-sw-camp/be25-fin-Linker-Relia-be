package com.linker.relia.customer.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.customer.dto.CustomerDetailResponse;
import com.linker.relia.customer.dto.CustomerListRequest;
import com.linker.relia.customer.dto.CustomerListResponse;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerListResponse>> getCustomers(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                          @Valid @ModelAttribute CustomerListRequest request) {
        CustomerListResponse responseDto = customerService.getCustomers(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "고객 목록 조회 성공", responseDto);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('FP', 'BRANCH_MANAGER', 'HQ_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerDetail(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                 @PathVariable UUID customerId) {
        CustomerDetailResponse responseDto = customerService.getCustomerDetail(principalDetails, customerId);
        return ApiResponse.success(HttpStatus.OK, "고객 상세 조회 성공", responseDto);
    }
}
