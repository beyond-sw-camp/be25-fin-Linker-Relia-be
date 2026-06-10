package com.linker.relia.handover.repository;

import com.linker.relia.handover.domain.ApprovalStatus;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HandoverRecommendationRepository // 추천 엔티티 저장/조회용 JPA repository
        extends JpaRepository<HandoverRecommendation, String> {

    // 특정 요청의 추천 설계사 조회 (결재 화면에서 사용)
    // ApprovalStatus.PENDING 으로 조회하면 현재 결재 대기 중인 추천 설계사 조회
    Optional<HandoverRecommendation> findByHandoverRequestAndApprovalStatus(
            HandoverRequest handoverRequest,
            ApprovalStatus approvalStatus
    );
}
