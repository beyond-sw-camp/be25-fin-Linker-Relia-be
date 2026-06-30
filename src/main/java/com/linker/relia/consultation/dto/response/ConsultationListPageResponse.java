package com.linker.relia.consultation.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class ConsultationListPageResponse extends PageImpl<ConsultationListResponse> {
    private final long contractCount;

    public ConsultationListPageResponse(Page<ConsultationListResponse> consultations,
                                        long contractCount) {
        super(consultations.getContent(), consultations.getPageable(), consultations.getTotalElements());
        this.contractCount = contractCount;
    }

    public long getContractCount() {
        return contractCount;
    }
}
