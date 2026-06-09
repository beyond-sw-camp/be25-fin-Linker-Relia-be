package com.linker.relia.consultation.controller;

import com.linker.relia.consultation.dto.request.ConsultationDraftSaveRequest;
import com.linker.relia.consultation.dto.response.ConsultationDraftResponse;
import com.linker.relia.consultation.service.ConsultationDraftService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultation-drafts")
@RequiredArgsConstructor
public class ConsultationDraftController {

    private final ConsultationDraftService consultationDraftService;

    @PostMapping
    public ConsultationDraftResponse saveDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody ConsultationDraftSaveRequest request
    ) {
        UUID fpId = principalDetails.getUser().getId();
        return consultationDraftService.saveDraft(fpId, request);
    }

    @GetMapping("/{draftId}")
    public ConsultationDraftResponse getDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID draftId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        return consultationDraftService.getDraft(draftId, fpId);
    }

    @DeleteMapping("/{draftId}")
    public void deleteDraft(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable UUID draftId
    ) {
        UUID fpId = principalDetails.getUser().getId();
        consultationDraftService.deleteDraft(draftId, fpId);
    }
}
