package com.linker.relia.consultation.service;

import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.user.domain.User;

public interface ConsultationService {
    ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    );
}
