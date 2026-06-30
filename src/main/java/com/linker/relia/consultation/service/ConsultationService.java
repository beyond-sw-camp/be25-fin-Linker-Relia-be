package com.linker.relia.consultation.service;

import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationDetailResponse;
import com.linker.relia.consultation.dto.response.ConsultationListPageResponse;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConsultationService {
    ConsultationCreateResponse createConsultation(
            ConsultationCreateRequest request,
            User fp
    );

    ConsultationListPageResponse getConsultations(Pageable pageable,
                                                  PrincipalDetails principalDetails,
                                                  String organizationCode);

    ConsultationDetailResponse getConsultationDetail(
            UUID consultationId,
            User fp
    );
}
