package com.linker.relia.handover.domain;

public enum RequestStatus {
    MANAGER_PENDING,  // 결재 대기
    COMPLETED,        // 변경 완료
    RETRY             // 재추천 대기
}