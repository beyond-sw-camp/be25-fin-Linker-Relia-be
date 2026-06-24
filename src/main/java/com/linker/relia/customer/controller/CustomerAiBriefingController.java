package com.linker.relia.customer.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.service.ai.ConsultationAiBriefingService;
import com.linker.relia.customer.dto.CustomerAiBriefingResponse;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerAiBriefingController {

    private final ConsultationAiBriefingService consultationAiBriefingService;

    @PostMapping("/api/customers/{customerId}/ai-briefing")
    public ResponseEntity<ApiResponse<CustomerAiBriefingResponse>> generateAiBriefing(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID customerId
    ) {
        CustomerAiBriefingResponse response =
                consultationAiBriefingService.generateAiBriefing(principalDetails, customerId);

        return ApiResponse.success(
                HttpStatus.OK,
                "AI 상담 브리핑 생성 성공",
                response
        );
    }

    @GetMapping("/api/customers/{customerId}/ai-briefing")
    public ResponseEntity<ApiResponse<CustomerAiBriefingResponse>> getLatestAiBriefing(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID customerId
    ) {
        CustomerAiBriefingResponse response =
                consultationAiBriefingService.getLatestAiBriefing(principalDetails, customerId);

        return ApiResponse.success(
                HttpStatus.OK,
                "AI 상담 브리핑 조회 성공",
                response
        );
    }
}
