package com.linker.relia.consultation.domain;

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
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultation_drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConsultationDraft {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fp_id", nullable = false)
    private User fp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false, length = 30)
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_channel", nullable = false, length = 30)
    private ConsultationChannel consultationChannel;

    @Column(name = "consulted_at", nullable = false)
    private LocalDateTime consultedAt;

    @Column(name = "special_note", columnDefinition = "TEXT")
    private String specialNote;

    @Column(name = "next_scheduled_at")
    private LocalDateTime nextScheduledAt;

    @Column(name = "draft_data", nullable = false, columnDefinition = "JSON")
    private String draftData;

    @Column(name = "last_saved_at", nullable = false)
    private LocalDateTime lastSavedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID deletedBy;

    public void updateDraft(
            Customer customer,
            Contract contract,
            ConsultationType consultationType,
            ConsultationChannel consultationChannel,
            LocalDateTime consultedAt,
            String specialNote,
            LocalDateTime nextScheduledAt,
            String draftData,
            UUID updatedBy
    ) {
        this.customer = customer;
        this.contract = contract;
        this.consultationType = consultationType;
        this.consultationChannel = consultationChannel;
        this.consultedAt = consultedAt;
        this.specialNote = specialNote;
        this.nextScheduledAt = nextScheduledAt;
        this.draftData = draftData;
        this.lastSavedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }
}