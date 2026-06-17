CREATE TABLE consultation_audio_records (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NULL,
    fp_id CHAR(36) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_consultation_audio_records_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADED', 'FAILED')),
    CONSTRAINT chk_consultation_audio_records_file_size CHECK (file_size >= 0),
    CONSTRAINT fk_consultation_audio_records_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_consultation_audio_records_fp FOREIGN KEY (fp_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_audio_records_fp_created_at
    ON consultation_audio_records (fp_id, created_at);

CREATE INDEX idx_consultation_audio_records_customer_created_at
    ON consultation_audio_records (customer_id, created_at);

CREATE INDEX idx_consultation_audio_records_upload_status
    ON consultation_audio_records (upload_status, created_at);

CREATE TABLE consultation_ai_drafts (
    id CHAR(36) NOT NULL,
    audio_record_id CHAR(36) NOT NULL,
    consultation_type VARCHAR(30) NOT NULL,
    draft_status VARCHAR(20) NOT NULL,
    stt_raw_text LONGTEXT NULL,
    gpt_summary_text LONGTEXT NULL,
    gpt_structured_data JSON NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by CHAR(36) NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_consultation_ai_drafts_type CHECK (consultation_type IN ('NEW_CONTRACT', 'CLAIM', 'TERMINATION', 'RENEWAL')),
    CONSTRAINT chk_consultation_ai_drafts_status CHECK (draft_status IN ('PENDING', 'STT_COMPLETED', 'GPT_COMPLETED', 'APPLIED', 'FAILED')),
    CONSTRAINT fk_consultation_ai_drafts_audio_record FOREIGN KEY (audio_record_id) REFERENCES consultation_audio_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_ai_drafts_audio_record_created_at
    ON consultation_ai_drafts (audio_record_id, created_at);

CREATE INDEX idx_consultation_ai_drafts_status_created_at
    ON consultation_ai_drafts (draft_status, created_at);
