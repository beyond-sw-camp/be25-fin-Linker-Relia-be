package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.service.ConsultationService;
import com.linker.relia.security.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationCreateResponse>> createConsultation(
            @Valid @RequestBody ConsultationCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
            ) {
        UUID fpId = principalDetails.getUser().getId();

        ConsultationCreateResponse response =
                consultationService.createConsultation(request,fpId);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "상담일지가 등록되었습니다.",
                response
        );
    }
}
