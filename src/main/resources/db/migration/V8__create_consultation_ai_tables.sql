CREATE TABLE consultation_audio_records (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    fp_id CHAR(36) NOT NULL,
    contract_id CHAR(36) NULL,
    consultation_type VARCHAR(30) NOT NULL,
    consultation_channel VARCHAR(30) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_status VARCHAR(20) NOT NULL,
    stt_status VARCHAR(20) NOT NULL,
    stt_provider VARCHAR(30) NULL,
    stt_raw_text LONGTEXT NULL,
    stt_confidence DECIMAL(5,2) NULL,
    stt_started_at DATETIME NULL,
    stt_completed_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_consultation_audio_records_type CHECK (consultation_type IN ('NEW_CONTRACT', 'CLAIM', 'TERMINATION', 'RENEWAL')),
    CONSTRAINT chk_consultation_audio_records_channel CHECK (consultation_channel IN ('VISIT', 'PHONE', 'MESSAGE')),
    CONSTRAINT chk_consultation_audio_records_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED')),
    CONSTRAINT chk_consultation_audio_records_stt_status CHECK (stt_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_consultation_audio_records_stt_provider CHECK (stt_provider IS NULL OR stt_provider IN ('CLOVA')),
    CONSTRAINT chk_consultation_audio_records_file_size CHECK (file_size >= 0),
    CONSTRAINT fk_consultation_audio_records_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_consultation_audio_records_fp FOREIGN KEY (fp_id) REFERENCES users(id),
    CONSTRAINT fk_consultation_audio_records_contract FOREIGN KEY (contract_id) REFERENCES contracts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_audio_records_fp_created_at
    ON consultation_audio_records (fp_id, created_at);

CREATE INDEX idx_consultation_audio_records_customer_created_at
    ON consultation_audio_records (customer_id, created_at);

CREATE INDEX idx_consultation_audio_records_upload_stt_status
    ON consultation_audio_records (upload_status, stt_status);

CREATE TABLE consultation_ai_drafts (
    id CHAR(36) NOT NULL,
    audio_record_id CHAR(36) NOT NULL,
    consultation_draft_id CHAR(36) NULL,
    consultation_type VARCHAR(30) NOT NULL,
    prompt_version VARCHAR(50) NULL,
    model_name VARCHAR(100) NULL,
    draft_status VARCHAR(20) NOT NULL,
    raw_transcript_snapshot LONGTEXT NOT NULL,
    gpt_summary_text LONGTEXT NULL,
    gpt_structured_data JSON NULL,
    applied_payload JSON NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_consultation_ai_drafts_type CHECK (consultation_type IN ('NEW_CONTRACT', 'CLAIM', 'TERMINATION', 'RENEWAL')),
    CONSTRAINT chk_consultation_ai_drafts_status CHECK (draft_status IN ('GENERATED', 'REVIEWED', 'APPLIED', 'FAILED')),
    CONSTRAINT fk_consultation_ai_drafts_audio_record FOREIGN KEY (audio_record_id) REFERENCES consultation_audio_records(id),
    CONSTRAINT fk_consultation_ai_drafts_consultation_draft FOREIGN KEY (consultation_draft_id) REFERENCES consultation_drafts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_ai_drafts_audio_record_created_at
    ON consultation_ai_drafts (audio_record_id, created_at);

CREATE INDEX idx_consultation_ai_drafts_consultation_draft
    ON consultation_ai_drafts (consultation_draft_id);

CREATE INDEX idx_consultation_ai_drafts_status_created_at
    ON consultation_ai_drafts (draft_status, created_at);
