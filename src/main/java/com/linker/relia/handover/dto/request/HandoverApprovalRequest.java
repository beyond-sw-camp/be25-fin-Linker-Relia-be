package com.linker.relia.handover.dto.request;

public record HandoverApprovalRequest(
        HandoverApprovalDecision approvalStatus,
        String rejectionReason
) {}
