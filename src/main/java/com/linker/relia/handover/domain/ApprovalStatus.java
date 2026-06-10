package com.linker.relia.handover.domain;

public enum ApprovalStatus {
    PENDING,   // 결재 대기
    APPROVED,  // 승인
    REJECTED   // 반려 (재추천으로 이어짐)
}