package com.linker.relia.consultation.controller;

import com.linker.relia.consultation.dto.request.ConsultationDraftSaveRequest;
import com.linker.relia.consultation.dto.response.ConsultationDraftResponse;
import com.linker.relia.consultation.service.ConsultationDraftService;
import com.linker.relia.security.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultation-drafts")
@RequiredArgsConstructor
public class ConsultationDraftController {

    private final ConsultationDraftService consultationDraftService;

    @Operation(summary = "상담일지 임시저장")
    @PostMapping
    public ConsultationDraftResponse saveDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody ConsultationDraftSaveRequest request
    ) {
        UUID fpId = principalDetails.getUser().getId();
        return consultationDraftService.saveDraft(fpId, request);
    }

    @Operation(summary = "임시저장 상담일지 목록 조회")
    @GetMapping
    public List<ConsultationDraftResponse> getDrafts(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ){
        UUID fpId = principalDetails.getUser().getId();
        return consultationDraftService.getDrafts(fpId);
    }

    @Operation(summary = "임시저장 상담일지 상세 조회")
    @GetMapping("/{draftId}")
    public ConsultationDraftResponse getDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID draftId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        return consultationDraftService.getDraft(draftId, fpId);
    }

    @Operation(summary = "임시저장 상담일지 삭제")
    @DeleteMapping("/{draftId}")
    public void deleteDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID draftId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        consultationDraftService.deleteDraft(draftId, fpId);
    }
}
