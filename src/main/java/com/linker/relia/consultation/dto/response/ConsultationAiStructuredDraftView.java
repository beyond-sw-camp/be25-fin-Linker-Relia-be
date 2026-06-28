package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ConsultationAiStructuredDraftView {
    private UUID customerId;
    private UUID contractId;
    private ConsultationType consultationType;
    private String consultationChannel;
    private LocalDateTime consultedAt;
    private String consultationContent;
    private String specialNote;
    private LocalDateTime nextScheduledAt;
    private ConsultationAiStructuredDraft.CustomerInfo customerInfo;
    private ConsultationAiStructuredDraft.NewDetail newDetail;
    private ClaimDetail claimDetail;
    private RenewalDetail renewalDetail;
    private CancelDetail cancelDetail;
    private ConsultationAiStructuredDraft.AiHints aiHints;

    @Getter
    @Builder
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
    @Builder
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

        private String changeType;
        private List<String> premiumChangeReasons;
        private List<String> customerResponses;
        private List<String> customerInterests;
        private String result;
    }

    @Getter
    @Builder
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
