package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationListResponse;
import com.linker.relia.consultation.service.ConsultationService;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(summary = "상담일지 유형별 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationCreateResponse>> createConsultation(
            @Valid @RequestBody ConsultationCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationCreateResponse response =
                consultationService.createConsultation(request, fp);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "상담일지가 등록되었습니다.",
                response
        );
    }

    @Operation(summary = "상담일지 목록조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConsultationListResponse>>> getConsultations(
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ConsultationListResponse> response =
                consultationService.getConsultations(pageable);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담일지 목록 조회에 성공했습니다.",
                response
        );
    }
}
