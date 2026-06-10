package com.linker.relia.consultation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "consultation_claim_details")
public class ConsultationClaimDetail {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @Column(name = "claim_stage", nullable = false, length = 30)
    private String claimStage;

    @Column(name = "claim_event_date")
    private LocalDate claimEventDate;

    @Column(name = "claim_reason_detail", length = 500)
    private String claimReasonDetail;

    @Column(name = "hospital_name", length = 100)
    private String hospitalName;

    @Column(name = "diagnosis_or_treatment", length = 500)
    private String diagnosisOrTreatment;

    @Column(name = "hospitalization_status", length = 30)
    private String hospitalizationStatus;

    @Column(name = "surgery_status", length = 30)
    private String surgeryStatus;

    @Column(name = "claim_result", nullable = false, length = 30)
    private String claimResult;

    @Column(name = "guidance_summary", length = 500)
    private String guidanceSummary;

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