package com.linker.relia.handover.dto.request;

import com.linker.relia.handover.domain.ApprovalStatus;

public record HandoverApprovalRequest(
        ApprovalStatus approvalStatus,   // APPROVED / REJECTED
        String rejectionReason           // nullable (REJECTED 시 선택 입력)
) {}