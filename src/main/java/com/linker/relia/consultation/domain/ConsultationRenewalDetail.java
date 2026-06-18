package com.linker.relia.consultation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "consultation_renewal_details")
public class ConsultationRenewalDetail {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(name = "renewal_reason", nullable = false, length = 50)
    private String renewalReason;

    @Column(name = "next_actions", length = 500)
    private String nextActions;

    @Column(name = "decision_expected_date")
    private LocalDate decisionExpectedDate;

    @Column(name = "renewal_scheduled_date", nullable = false)
    private LocalDate renewalScheduledDate;

    @Column(name = "current_premium", nullable = false)
    private BigDecimal currentPremium;

    @Column(name = "renewal_premium", nullable = false)
    private BigDecimal renewalPremium;

    @Column(name = "premium_change_rate", nullable = false)
    private BigDecimal premiumChangeRate;

    @Column(name = "coverage_change_type", nullable = false, length = 30)
    private String coverageChangeType;

    @Column(name = "coverage_change_detail", length = 500)
    private String coverageChangeDetail;

    @Column(name = "customer_reaction", nullable = false, length = 30)
    private String customerReaction;

    @Column(name = "consultation_result", nullable = false, length = 30)
    private String consultationResult;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "deleted_by")
    private UUID deletedBy;
}