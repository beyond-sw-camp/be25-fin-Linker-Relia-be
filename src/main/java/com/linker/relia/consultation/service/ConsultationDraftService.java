package com.linker.relia.consultation.service;

import com.linker.relia.consultation.dto.request.ConsultationDraftSaveRequest;
import com.linker.relia.consultation.dto.response.ConsultationDraftResponse;

import java.util.UUID;

public interface ConsultationDraftService {

    ConsultationDraftResponse saveDraft(
            UUID fpId,
            ConsultationDraftSaveRequest request
    );

    ConsultationDraftResponse getDraft(
            UUID draftId,
            UUID fpId
    );

    void deleteDraft(
            UUID draftId,
            UUID fpId
    );
}
