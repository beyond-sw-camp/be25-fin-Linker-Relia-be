package com.linker.relia.consultation.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.consultation.dto.response.ConsultationAiBriefingSourceResponse;
import com.linker.relia.consultation.dto.response.ConsultationHistoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ConsultationRepositoryCustom {
    Page<ConsultationHistoryItemResponse> findOwnCustomerConsultations(AccessScope accessScope,
                                                                       UUID customerId,
                                                                       Pageable pageable);
    List<ConsultationAiBriefingSourceResponse> findConsultationsForAiBriefing(
            AccessScope accessScope,
            UUID customerId,
            int limit
    );
}
