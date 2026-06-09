package com.linker.relia.consultation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "consultation_renewal_premium_change_reasons")
public class ConsultationRenewalPremiumChangeReason {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_renewal_detail_id", nullable = false)
    private ConsultationRenewalDetail consultationRenewalDetail;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType;

    @Column(name = "other_reason", length = 50)
    private String otherReason;

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