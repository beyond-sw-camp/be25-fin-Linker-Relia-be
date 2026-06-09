package com.linker.relia.consultation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class ConsultationClaimDetailRequest {

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
}
