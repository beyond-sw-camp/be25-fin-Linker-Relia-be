package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ConsultationAiStructuredDraft {
    private UUID customerId;
    private UUID contractId;
    private ConsultationType consultationType;
    private String consultationChannel;
    private LocalDateTime consultedAt;
    private String consultationContent;
    private String specialNote;
    private LocalDateTime nextScheduledAt;
    private CustomerInfo customerInfo;
    private NewDetail newDetail;
    private ClaimDetail claimDetail;
    private RenewalDetail renewalDetail;
    private CancelDetail cancelDetail;
    private AiHints aiHints;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AiHints {
        private String targetContractHint;
        private List<String> mentionedProductNames;
        private List<String> mentionedDiseaseNames;
        private String claimTypeHint;
        private List<String> claimReviewItemHints;
        private String claimResultHint;
        private List<String> claimNextActionHints;
        private String claimHospitalizationStatusHint;
        private String claimSurgeryStatusHint;
        private String renewalConsultationResultHint;
        private String renewalCoverageChangeTypeHint;
        private String renewalCustomerReactionHint;
        private List<String> renewalInterestTypeHints;
        private List<String> renewalPremiumChangeReasonTypeHints;
        private List<String> renewalNextActionHints;
        private List<String> cancellationReviewReasonHints;
        private List<String> cancellationRetentionPlanHints;
        private String cancellationCustomerIntentHint;
        private String cancellationResultHint;
        private List<String> cancellationNextActionHints;
        private String terminationRetentionPossibilityHint;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CustomerInfo {
        private String customerName;
        private String customerGender;
        private LocalDate customerBirthDate;
        private String customerPhone;
        private String customerEmail;
        private String customerZipcode;
        private String customerAddressRoad;
        private String customerAddressDetail;
        private String customerJob;
        private String customerCompanyName;
        private Long customerAnnualIncome;
        private Long customerAssetSize;
        private String customerDebtStatus;
        private Boolean customerIsSmoker;
        private Boolean customerIsDrinker;
        private String customerMaritalStatus;
        private Integer customerDependentsCount;
        private List<String> underlyingDiseaseCodes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class NewDetail {
        private Long monthlyIncome;
        private Boolean hasExistingInsurance;
        private Long monthlyInsurancePremium;
        private String existingInsuranceNote;
        private String insurancePriority;
        private List<String> coverageTypes;
        private List<String> proposedProductCodes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClaimDetail {
        private String claimType;
        private String claimReason;
        private LocalDate incidentDate;
        private String hospitalName;
        private String diagnosisOrTreatment;
        private String hospitalizationStatus;
        private String surgeryStatus;
        private Long claimAmount;
        private List<String> reviewItems;
        private String result;
        private List<String> nextActions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RenewalDetail {
        private String renewalReason;
        private LocalDate renewalScheduledDate;
        private Long currentPremium;
        private Long renewalPremium;
        private BigDecimal premiumChangeRate;
        private String coverageChangeType;
        private String coverageChangeDetail;
        private String customerReaction;
        private List<String> interestTypes;
        private String consultationResult;
        private List<String> premiumChangeReasonTypes;
        private List<String> nextActions;
        private LocalDate decisionExpectedDate;
        private String otherReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CancelDetail {
        private List<String> reviewReasons;
        private String reasonDetail;
        private List<String> retentionPlans;
        private String customerIntent;
        private String retentionPossibility;
        private String result;
        private List<String> nextActions;
        private Boolean premiumBurden;
        private Boolean renewalPremiumBurden;
        private Boolean paymentDifficulty;
        private Boolean coverageDissatisfaction;
        private Boolean duplicateCoverage;
        private Boolean productRemodelingReview;
        private Boolean comparingOtherCompany;
        private Boolean movingToOtherCompany;
        private Boolean plannerContactDissatisfaction;
        private Boolean managementDissatisfaction;
    }
}
