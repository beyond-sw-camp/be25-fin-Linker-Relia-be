package com.linker.relia.consultation.dto.request;

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

    private String reasonDetail;

    private List<String> retentionPlans;

    private String customerIntent;

    private String result;

    private List<String> nextActions;
}
