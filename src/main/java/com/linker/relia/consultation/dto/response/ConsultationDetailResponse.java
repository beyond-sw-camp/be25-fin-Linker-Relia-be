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
public class ConsultationDetailResponse {

    private UUID consultationId;

    private UUID customerId;
    private String customerName;

    private UUID contractId;

    private ConsultationType consultationType;
    private ConsultationChannel consultationChannel;

    private LocalDateTime consultedAt;
    private LocalDateTime nextScheduledAt;

    private String specialNote;
    private String fpName;

    private NewDetailResponse newDetail;
    private RenewalDetailResponse renewalDetail;
    private ClaimDetailResponse claimDetail;

    public static ConsultationDetailResponse from(
            Consultation consultation,
            NewDetailResponse newDetail,
            RenewalDetailResponse renewalDetail,
            ClaimDetailResponse claimDetail
    ) {
        return ConsultationDetailResponse.builder()
                .consultationId(consultation.getId())
                .customerId(consultation.getCustomer().getId())
                .customerName(consultation.getCustomer().getCustomerName())
                .contractId(
                        consultation.getContract() != null
                                ? consultation.getContract().getId()
                                : null
                )
                .consultationType(consultation.getConsultationType())
                .consultationChannel(consultation.getConsultationChannel())
                .consultedAt(consultation.getConsultedAt())
                .nextScheduledAt(consultation.getNextScheduledAt())
                .specialNote(consultation.getSpecialNote())
                .fpName(consultation.getFp().getUserName())
                .newDetail(newDetail)
                .renewalDetail(renewalDetail)
                .claimDetail(claimDetail)
                .build();
    }
}