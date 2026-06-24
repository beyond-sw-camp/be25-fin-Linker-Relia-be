package com.linker.relia.consultation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ConsultationCancelDetailRequest {

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

    private List<String> reviewReasons;

    @Size(max = 500, message = "해지 사유 상세는 500자 이하여야 합니다.")
    private String reasonDetail;

    private List<String> retentionPlans;

    @Size(max = 100, message = "고객 의향은 100자 이하여야 합니다.")
    private String customerIntent;

    @Size(max = 100, message = "상담 결과는 100자 이하여야 합니다.")
    private String result;

    private List<String> nextActions;
}
