package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationClaimDetail;
import com.linker.relia.consultation.domain.ConsultationClaimReviewItem;
import com.linker.relia.consultation.domain.ConsultationClaimType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ClaimDetailResponse {

    private String claimStage;
    private LocalDate claimEventDate;
    private String claimReasonDetail;
    private String hospitalName;
    private String diagnosisOrTreatment;
    private String hospitalizationStatus;
    private String surgeryStatus;
    private String claimResult;
    private String guidanceSummary;

    private List<String> claimTypes;
    private List<String> reviewTypes;

    public static ClaimDetailResponse from(
            ConsultationClaimDetail detail,
            List<ConsultationClaimType> claimTypes,
            List<ConsultationClaimReviewItem> reviewItems
    ) {
        return ClaimDetailResponse.builder()
                .claimStage(detail.getClaimStage())
                .claimEventDate(detail.getClaimEventDate())
                .claimReasonDetail(detail.getClaimReasonDetail())
                .hospitalName(detail.getHospitalName())
                .diagnosisOrTreatment(detail.getDiagnosisOrTreatment())
                .hospitalizationStatus(detail.getHospitalizationStatus())
                .surgeryStatus(detail.getSurgeryStatus())
                .claimResult(detail.getClaimResult())
                .guidanceSummary(detail.getGuidanceSummary())
                .claimTypes(
                        claimTypes.stream()
                                .map(ConsultationClaimType::getClaimType)
                                .toList()
                )
                .reviewTypes(
                        reviewItems.stream()
                                .map(ConsultationClaimReviewItem::getReviewType)
                                .toList()
                )
                .build();
    }
}