package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionCompleteRequest;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionStartRequest;
import com.linker.relia.consultation.dto.response.ConsultationSttSessionResponse;
import com.linker.relia.consultation.service.stt.ConsultationSttSessionService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultation-stt-sessions")
public class ConsultationSttSessionController {
    private final ConsultationSttSessionService consultationSttSessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationSttSessionResponse>> startSession(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody ConsultationSttSessionStartRequest request
    ) {
        UUID fpId = principalDetails.getUser().getId();
        ConsultationSttSessionResponse response = consultationSttSessionService.startSession(fpId, request);

        return ApiResponse.success(HttpStatus.CREATED, "STT 세션이 시작되었습니다.", response);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<ConsultationSttSessionResponse>> completeSession(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ConsultationSttSessionCompleteRequest request
    ) {
        UUID fpId = principalDetails.getUser().getId();
        ConsultationSttSessionResponse response =
                consultationSttSessionService.completeSession(sessionId, fpId, request);

        return ApiResponse.success(HttpStatus.OK, "STT 세션이 종료되었습니다.", response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<ConsultationSttSessionResponse>> getSession(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID sessionId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        ConsultationSttSessionResponse response = consultationSttSessionService.getSession(sessionId, fpId);

        return ApiResponse.success(HttpStatus.OK, "STT 세션 조회에 성공했습니다.", response);
    }
}
