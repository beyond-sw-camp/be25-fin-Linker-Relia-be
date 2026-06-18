package com.linker.relia.consultation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class ConsultationRenewalDetailRequest {

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

    private String nextActions;
    private LocalDate decisionExpectedDate;

}
