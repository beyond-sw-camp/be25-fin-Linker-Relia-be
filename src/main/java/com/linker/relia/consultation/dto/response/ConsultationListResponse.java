package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.consultation.domain.ConsultationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationListResponse {

    private UUID consultationId;
    private String customerName;
    private ConsultationType consultationType;
    private ConsultationChannel consultationChannel;
    private LocalDateTime consultedAt;
    private LocalDateTime nextScheduledAt;
    private String fpName;

    public static ConsultationListResponse from(Consultation consultation) {
        return ConsultationListResponse.builder()
                .consultationId(consultation.getId())
                .customerName(consultation.getCustomer().getCustomerName())
                .consultationType(consultation.getConsultationType())
                .consultationChannel(consultation.getConsultationChannel())
                .consultedAt(consultation.getConsultedAt())
                .nextScheduledAt(consultation.getNextScheduledAt())
                .fpName(consultation.getFp().getUserName())
                .build();
    }
}