package com.linker.relia.consultation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class ConsultationClaimDetailRequest {

    private String claimType;
    private String claimReason;
    private LocalDate incidentDate;
    private List<String> reviewItems;
    private String result;
    private List<String> nextActions;
}
