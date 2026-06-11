package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationCancelDetail;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelDetailResponse {

    private Boolean premiumBurden;
    private Boolean renewalPremiumBurden;
    private Boolean paymentDifficulty;
    private Boolean coverageDissatisfaction;
    private Boolean duplicateInsurance;
    private Boolean productRemodelingReview;
    private Boolean comparingOtherCompany;
    private Boolean movingToOtherCompany;
    private Boolean plannerContactDissatisfaction;
    private Boolean managementDissatisfaction;
    private String retentionPossibility;

    public static CancelDetailResponse from(ConsultationCancelDetail detail) {
        return CancelDetailResponse.builder()
                .premiumBurden(detail.getPremiumBurden())
                .renewalPremiumBurden(detail.getRenewalPremiumBurden())
                .paymentDifficulty(detail.getPaymentDifficulty())
                .coverageDissatisfaction(detail.getCoverageDissatisfaction())
                .duplicateInsurance(detail.getDuplicateInsurance())
                .productRemodelingReview(detail.getProductRemodelingReview())
                .comparingOtherCompany(detail.getComparingOtherCompany())
                .movingToOtherCompany(detail.getMovingToOtherCompany())
                .plannerContactDissatisfaction(detail.getPlannerContactDissatisfaction())
                .managementDissatisfaction(detail.getManagementDissatisfaction())
                .retentionPossibility(detail.getRetentionPossibility())
                .build();
    }
}