package com.linker.relia.consultation.domain;

import com.linker.relia.insurance.domain.InsuranceProduct;
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
@Table(name = "consultation_new_proposed_products")
public class ConsultationNewProposedProduct {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_new_detail_id", nullable = false)
    private ConsultationNewDetail consultationNewDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_product_id", nullable = false)
    private InsuranceProduct insuranceProduct;

    @Column(name = "insurance_product_name", nullable = false, length = 200)
    private String insuranceProductName;

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