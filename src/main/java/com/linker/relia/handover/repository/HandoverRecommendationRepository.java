package com.linker.relia.handover.repository;

import com.linker.relia.handover.domain.ApprovalStatus;
import com.linker.relia.handover.domain.HandoverRecommendation;
import com.linker.relia.handover.domain.HandoverRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HandoverRecommendationRepository
        extends JpaRepository<HandoverRecommendation, UUID> {

    @Query("""
        SELECT r
        FROM HandoverRecommendation r
        WHERE r.handoverRequest = :handoverRequest
          AND (:approvalStatus IS NULL OR r.approvalStatus = :approvalStatus)
        ORDER BY r.createdAt DESC
        """)
    List<HandoverRecommendation> findLatestByHandoverRequestAndApprovalStatus(
            @Param("handoverRequest") HandoverRequest handoverRequest,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            Pageable pageable
    );
}
