package com.linker.relia.consultation.domain.stt;

import com.linker.relia.common.domain.BaseEntity;
import com.linker.relia.consultation.domain.ConsultationType;
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
@Table(name = "consultation_stt_sessions")
public class ConsultationSttSession extends BaseEntity {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fp_id")
    private User fp;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type")
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private ConsultationSttSessionStatus sessionStatus;

    @Column(name = "partial_text")
    private String partialText;

    @Column(name = "final_text")
    private String finalText;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "deleted_by")
    private UUID deletedBy;

    public void updatePartialText(String partialText) {
        this.partialText = partialText;
    }

    public void markProcessing() {
        this.sessionStatus = ConsultationSttSessionStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void start() {
        this.sessionStatus = ConsultationSttSessionStatus.RECORDING;
        this.errorMessage = null;
    }

    public void complete(String finalText, LocalDateTime endedAt) {
        this.finalText = finalText;
        this.endedAt = endedAt;
        this.sessionStatus = ConsultationSttSessionStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void fail(String errorMessage, LocalDateTime endedAt) {
        this.errorMessage = errorMessage;
        this.endedAt = endedAt;
        this.sessionStatus = ConsultationSttSessionStatus.FAILED;
    }

    public void delete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
