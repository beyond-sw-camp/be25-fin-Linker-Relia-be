package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiNoteApplyResponse;
import com.linker.relia.consultation.service.stt.ConsultationAiNoteService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultation-ai-notes")
public class ConsultationAiNoteController {
    private final ConsultationAiNoteService consultationAiNoteService;

    @PatchMapping("/{aiNoteId}/apply")
    public ResponseEntity<ApiResponse<ConsultationAiNoteApplyResponse>> applyAiDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID aiNoteId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        ConsultationAiNoteApplyResponse response = consultationAiNoteService.applyAiDraft(aiNoteId, fpId);

        return ApiResponse.success(HttpStatus.OK, "AI 상담 초안이 상담일지에 반영되었습니다.", response);
    }
}
