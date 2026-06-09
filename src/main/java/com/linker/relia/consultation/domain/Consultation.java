package com.linker.relia.consultation.domain;

import com.linker.relia.common.domain.BaseEntity;
import com.linker.relia.contract.domain.Contract;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "consultations")
public class Consultation extends BaseEntity {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "consultation_sequence")
    private Integer consultationSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fp_id")
    private User fp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type")
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_channel")
    private ConsultationChannel consultationChannel;

    @Column(name = "consulted_at")
    private LocalDateTime consultedAt;

    @Column(name = "special_note")
    private String specialNote;

    @Column(name = "next_scheduled_at")
    private LocalDateTime nextScheduledAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "deleted_by")
    private UUID deletedBy;
}
