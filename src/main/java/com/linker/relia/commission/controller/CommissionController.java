package com.linker.relia.commission.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.commission.dto.FpCommissionSummaryRequest;
import com.linker.relia.commission.dto.FpCommissionSummaryResponse;
import com.linker.relia.commission.service.CommissionService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/commissions")
public class CommissionController {
    private final CommissionService commissionService;

    @GetMapping("/fp-summary")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<FpCommissionSummaryResponse>> getFpCommissionSummary(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                                                           @Valid @ModelAttribute FpCommissionSummaryRequest request) {
        FpCommissionSummaryResponse response = commissionService.getFpCommissionSummary(principalDetails, request);
        return ApiResponse.success(HttpStatus.OK, "FP commission summary retrieved successfully.", response);
    }
}
