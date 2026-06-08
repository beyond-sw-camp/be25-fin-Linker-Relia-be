package com.linker.relia.handover.domain;

import com.linker.relia.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "handover_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HandoverRecommendation {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handover_request_id", nullable = false)
    // 여러 추천이 하나의 요청에 연결 (재추천 시 같은 요청에 새 추천이 쌓임)
    private HandoverRequest handoverRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_fp_id", nullable = false)
    private User recommendedFp;

    @Column(name = "recommended_fp_name", length = 50, nullable = false)
    private String recommendedFpName;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30, nullable = false)
    private ApprovalStatus approvalStatus;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //  정적 팩토리 메서드
    public static HandoverRecommendation create(HandoverRequest handoverRequest, User recommendedFp,
                                                String recommendationReason) {
        HandoverRecommendation rec = new HandoverRecommendation();
        rec.id = UUID.randomUUID().toString();
        rec.handoverRequest = handoverRequest;
        rec.recommendedFp = recommendedFp;
        rec.recommendedFpName = recommendedFp.getUserName();
        rec.recommendationReason = recommendationReason;
        rec.approvalStatus = ApprovalStatus.PENDING;
        rec.createdAt = LocalDateTime.now();
        return rec;
    }

    //  비즈니스 메서드
    public void approve(String reviewedBy) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.reviewedBy = reviewedBy;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String reviewedBy) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.reviewedBy = reviewedBy;
        this.rejectedAt = LocalDateTime.now();
    }
}