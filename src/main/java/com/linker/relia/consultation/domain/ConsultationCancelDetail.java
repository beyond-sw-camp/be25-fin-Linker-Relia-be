package com.linker.relia.consultation.domain;

import com.linker.relia.consultation.domain.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "consultation_cancel_details")
public class ConsultationCancelDetail {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(name = "premium_burden", nullable = false)
    private Boolean premiumBurden;

    @Column(name = "renewal_premium_burden", nullable = false)
    private Boolean renewalPremiumBurden;

    @Column(name = "payment_difficulty", nullable = false)
    private Boolean paymentDifficulty;

    @Column(name = "coverage_dissatisfaction", nullable = false)
    private Boolean coverageDissatisfaction;

    @Column(name = "duplicate_insurance", nullable = false)
    private Boolean duplicateInsurance;

    @Column(name = "product_remodeling_review", nullable = false)
    private Boolean productRemodelingReview;

    @Column(name = "comparing_other_company", nullable = false)
    private Boolean comparingOtherCompany;

    @Column(name = "moving_to_other_company", nullable = false)
    private Boolean movingToOtherCompany;

    @Column(name = "planner_contact_dissatisfaction", nullable = false)
    private Boolean plannerContactDissatisfaction;

    @Column(name = "management_dissatisfaction", nullable = false)
    private Boolean managementDissatisfaction;

    @Column(name = "retention_possibility", nullable = false, length = 30)
    private String retentionPossibility;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "review_reasons", columnDefinition = "LONGTEXT")
    private List<String> reviewReasons;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "retention_plans", columnDefinition = "LONGTEXT")
    private List<String> retentionPlans;

    @Column(name = "customer_intent", length = 100)
    private String customerIntent;

    @Column(name = "result", length = 100)
    private String result;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "next_actions", columnDefinition = "LONGTEXT")
    private List<String> nextActions;

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
