package com.linker.relia.consultation.domain;

import com.linker.relia.common.domain.BaseEntity;
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
@Table(name = "consultation_ai_drafts")
public class ConsultationAiDraft extends BaseEntity {
    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_record_id")
    private ConsultationAudioRecord audioRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type")
    private ConsultationType consultationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "draft_status")
    private ConsultationAiDraftStatus draftStatus;

    @Column(name = "stt_raw_text")
    private String sttRawText;

    @Column(name = "gpt_summary_text")
    private String gptSummaryText;

    @Column(name = "gpt_structured_data")
    private String gptStructuredData;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "deleted_by")
    private UUID deletedBy;

    public void completeStt(String sttRawText) {
        this.sttRawText = sttRawText;
        this.draftStatus = ConsultationAiDraftStatus.STT_COMPLETED;
        this.errorMessage = null;
    }

    public void completeGpt(String gptSummaryText, String gptStructuredData) {
        this.gptSummaryText = gptSummaryText;
        this.gptStructuredData = gptStructuredData;
        this.draftStatus = ConsultationAiDraftStatus.GPT_COMPLETED;
        this.errorMessage = null;
    }

    public void markApplied() {
        this.draftStatus = ConsultationAiDraftStatus.APPLIED;
    }

    public void markFailed(String errorMessage) {
        this.draftStatus = ConsultationAiDraftStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void delete(UUID deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
