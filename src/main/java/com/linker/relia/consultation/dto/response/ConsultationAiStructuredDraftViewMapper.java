package com.linker.relia.consultation.dto.response;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConsultationAiStructuredDraftViewMapper {

    public ConsultationAiStructuredDraftView toView(ConsultationAiStructuredDraft draft) {
        if (draft == null) {
            return null;
        }

        return ConsultationAiStructuredDraftView.builder()
                .customerId(draft.getCustomerId())
                .contractId(draft.getContractId())
                .consultationType(draft.getConsultationType())
                .consultationChannel(draft.getConsultationChannel())
                .consultedAt(draft.getConsultedAt())
                .consultationContent(draft.getConsultationContent())
                .specialNote(draft.getSpecialNote())
                .nextScheduledAt(draft.getNextScheduledAt())
                .customerInfo(draft.getCustomerInfo())
                .newDetail(draft.getNewDetail())
                .claimDetail(toClaimDetail(draft.getClaimDetail()))
                .renewalDetail(toRenewalDetail(draft.getRenewalDetail()))
                .cancelDetail(toCancelDetail(draft.getCancelDetail()))
                .aiHints(draft.getAiHints())
                .build();
    }

    private ConsultationAiStructuredDraftView.ClaimDetail toClaimDetail(ConsultationAiStructuredDraft.ClaimDetail claimDetail) {
        if (claimDetail == null) {
            return null;
        }
        return ConsultationAiStructuredDraftView.ClaimDetail.builder()
                .claimType(claimDetail.getClaimType())
                .claimReason(claimDetail.getClaimReason())
                .incidentDate(claimDetail.getIncidentDate())
                .hospitalName(claimDetail.getHospitalName())
                .diagnosisOrTreatment(claimDetail.getDiagnosisOrTreatment())
                .hospitalizationStatus(claimDetail.getHospitalizationStatus())
                .surgeryStatus(claimDetail.getSurgeryStatus())
                .claimAmount(claimDetail.getClaimAmount())
                .reviewItems(claimDetail.getReviewItems())
                .result(claimDetail.getResult())
                .nextActions(claimDetail.getNextActions())
                .build();
    }

    private ConsultationAiStructuredDraftView.RenewalDetail toRenewalDetail(ConsultationAiStructuredDraft.RenewalDetail renewalDetail) {
        if (renewalDetail == null) {
            return null;
        }
        List<String> customerResponses = renewalDetail.getCustomerReaction() == null
                ? null
                : List.of(renewalDetail.getCustomerReaction());
        return ConsultationAiStructuredDraftView.RenewalDetail.builder()
                .renewalReason(renewalDetail.getRenewalReason())
                .renewalScheduledDate(renewalDetail.getRenewalScheduledDate())
                .currentPremium(renewalDetail.getCurrentPremium())
                .renewalPremium(renewalDetail.getRenewalPremium())
                .premiumChangeRate(renewalDetail.getPremiumChangeRate())
                .coverageChangeType(renewalDetail.getCoverageChangeType())
                .coverageChangeDetail(renewalDetail.getCoverageChangeDetail())
                .customerReaction(renewalDetail.getCustomerReaction())
                .interestTypes(renewalDetail.getInterestTypes())
                .consultationResult(renewalDetail.getConsultationResult())
                .premiumChangeReasonTypes(renewalDetail.getPremiumChangeReasonTypes())
                .nextActions(renewalDetail.getNextActions())
                .decisionExpectedDate(renewalDetail.getDecisionExpectedDate())
                .otherReason(renewalDetail.getOtherReason())
                .changeType(renewalDetail.getCoverageChangeType())
                .premiumChangeReasons(renewalDetail.getPremiumChangeReasonTypes())
                .customerResponses(customerResponses)
                .customerInterests(renewalDetail.getInterestTypes())
                .result(renewalDetail.getConsultationResult())
                .build();
    }

    private ConsultationAiStructuredDraftView.CancelDetail toCancelDetail(ConsultationAiStructuredDraft.CancelDetail cancelDetail) {
        if (cancelDetail == null) {
            return null;
        }
        return ConsultationAiStructuredDraftView.CancelDetail.builder()
                .reviewReasons(cancelDetail.getReviewReasons())
                .reasonDetail(cancelDetail.getReasonDetail())
                .retentionPlans(cancelDetail.getRetentionPlans())
                .customerIntent(cancelDetail.getCustomerIntent())
                .retentionPossibility(cancelDetail.getRetentionPossibility())
                .result(cancelDetail.getResult())
                .nextActions(cancelDetail.getNextActions())
                .premiumBurden(cancelDetail.getPremiumBurden())
                .renewalPremiumBurden(cancelDetail.getRenewalPremiumBurden())
                .paymentDifficulty(cancelDetail.getPaymentDifficulty())
                .coverageDissatisfaction(cancelDetail.getCoverageDissatisfaction())
                .duplicateCoverage(cancelDetail.getDuplicateCoverage())
                .productRemodelingReview(cancelDetail.getProductRemodelingReview())
                .comparingOtherCompany(cancelDetail.getComparingOtherCompany())
                .movingToOtherCompany(cancelDetail.getMovingToOtherCompany())
                .plannerContactDissatisfaction(cancelDetail.getPlannerContactDissatisfaction())
                .managementDissatisfaction(cancelDetail.getManagementDissatisfaction())
                .build();
    }
}
