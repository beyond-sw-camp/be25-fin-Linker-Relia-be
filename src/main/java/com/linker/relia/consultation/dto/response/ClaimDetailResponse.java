package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationClaimDetail;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ClaimDetailResponse {

    private String claimType;
    private String claimReason;
    private String hospitalName;
    private String diagnosisOrTreatment;
    private String hospitalizationStatus;
    private String surgeryStatus;
    private LocalDate incidentDate;
    private List<String> reviewItems;
    private String result;
    private List<String> nextActions;

    public static ClaimDetailResponse from(ConsultationClaimDetail detail) {
        return ClaimDetailResponse.builder()
                .claimType(detail.getClaimType())
                .claimReason(detail.getClaimReason())
                .hospitalName(detail.getHospitalName())
                .diagnosisOrTreatment(detail.getDiagnosisOrTreatment())
                .hospitalizationStatus(detail.getHospitalizationStatus())
                .surgeryStatus(detail.getSurgeryStatus())
                .incidentDate(detail.getIncidentDate())
                .reviewItems(List.of())
                .result(detail.getClaimResult())
                .nextActions(List.of())
                .build();
    }
}