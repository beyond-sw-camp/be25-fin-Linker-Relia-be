ALTER TABLE consultation_ai_notes
    MODIFY COLUMN audio_record_id CHAR(36) NULL,
    ADD COLUMN stt_session_id CHAR(36) NULL AFTER audio_record_id,
    ADD CONSTRAINT fk_consultation_ai_notes_stt_session
        FOREIGN KEY (stt_session_id) REFERENCES consultation_stt_sessions(id);

CREATE UNIQUE INDEX uk_consultation_ai_notes_stt_session
    ON consultation_ai_notes (stt_session_id);
