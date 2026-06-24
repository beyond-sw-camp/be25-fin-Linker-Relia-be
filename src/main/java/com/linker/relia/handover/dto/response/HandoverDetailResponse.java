package com.linker.relia.handover.dto.response;

import com.linker.relia.consultation.domain.ConsultationChannel;
import com.linker.relia.customer.domain.CustomerGrade;
import com.linker.relia.handover.domain.ApprovalStatus;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HandoverDetailResponse(
        UUID handoverRequestId,
        RequestType requestType,
        RequestStatus requestStatus,
        LocalDateTime createdAt,
        CustomerInfo customer,
        HandoverHistoryInfo handoverHistory,
        RecommendationInfo recommendation,
        boolean canApprove
) {
    public record CustomerInfo(
            UUID customerId,
            String customerName,
            Integer customerAge,
            CustomerGrade customerGrade,
            String currentFpName,
            String contractSummary,        // "실손 2건 · 종신 1건"
            BigDecimal monthlyPremium,
            LocalDateTime lastConsultedAt,
            ConsultationChannel mainConsultationChannel

    ) {}

    public record HandoverHistoryInfo(
            String previousFpName,
            LocalDateTime changedAt
    ) {}

    public record RecommendationInfo(
            UUID recommendationId,
            String recommendedFpName,
            List<String> specialtyCategories,
            BigDecimal retentionRate,
            Integer preferredCustomerAge,
            String consultationChannel,
            String recommendationReason,
            ApprovalStatus approvalStatus,
            String rejectedFpName           // null이면 첫 추천, 있으면 재추천
    ) {}
}
