ALTER TABLE consultation_ai_briefings
    ADD COLUMN source_fingerprint CHAR(64) NOT NULL DEFAULT '' AFTER briefing_content;
