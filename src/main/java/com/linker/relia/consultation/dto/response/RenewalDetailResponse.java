package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationRenewalDetail;
import com.linker.relia.consultation.domain.ConsultationRenewalInterest;
import com.linker.relia.consultation.domain.ConsultationRenewalPremiumChangeReason;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class RenewalDetailResponse {

    private String renewalReason;
    private LocalDate renewalScheduledDate;
    private BigDecimal currentPremium;
    private BigDecimal renewalPremium;
    private BigDecimal premiumChangeRate;
    private String coverageChangeType;
    private String coverageChangeDetail;
    private String customerReaction;
    private String consultationResult;

    private List<String> premiumChangeReasonTypes;
    private String otherReason;
    private List<String> interestTypes;

    public static RenewalDetailResponse from(
            ConsultationRenewalDetail detail,
            List<ConsultationRenewalPremiumChangeReason> premiumChangeReasons,
            List<ConsultationRenewalInterest> interests
    ) {
        return RenewalDetailResponse.builder()
                .renewalReason(detail.getRenewalReason())
                .renewalScheduledDate(detail.getRenewalScheduledDate())
                .currentPremium(detail.getCurrentPremium())
                .renewalPremium(detail.getRenewalPremium())
                .premiumChangeRate(detail.getPremiumChangeRate())
                .coverageChangeType(detail.getCoverageChangeType())
                .coverageChangeDetail(detail.getCoverageChangeDetail())
                .customerReaction(detail.getCustomerReaction())
                .consultationResult(detail.getConsultationResult())
                .premiumChangeReasonTypes(
                        premiumChangeReasons.stream()
                                .map(ConsultationRenewalPremiumChangeReason::getReasonType)
                                .toList()
                )
                .otherReason(
                        premiumChangeReasons.stream()
                                .filter(reason -> "OTHER".equals(reason.getReasonType()))
                                .map(ConsultationRenewalPremiumChangeReason::getOtherReason)
                                .filter(reason -> reason != null && !reason.isBlank())
                                .findFirst()
                                .orElse(null)
                )
                .interestTypes(
                        interests.stream()
                                .map(ConsultationRenewalInterest::getInterestType)
                                .toList()
                )
                .build();
    }
}