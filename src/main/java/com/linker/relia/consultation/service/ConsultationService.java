package com.linker.relia.consultation.service;

import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationDetailResponse;
import com.linker.relia.consultation.dto.response.ConsultationListResponse;
import com.linker.relia.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConsultationService {
    ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    );

    Page<ConsultationListResponse> getConsultations(Pageable pageable);

    ConsultationDetailResponse getConsultationDetail(
            UUID consultationId,
            User fp
    );
}