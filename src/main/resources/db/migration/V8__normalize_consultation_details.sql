-- Normalize claim detail collections and reconcile schema drift created by Hibernate ddl-auto.

ALTER TABLE consultation_claim_details
    ADD COLUMN IF NOT EXISTS incident_date DATE NULL,
    ADD COLUMN IF NOT EXISTS claim_type VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS review_items VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS next_actions VARCHAR(500) NULL;

ALTER TABLE consultation_renewal_details
    ADD COLUMN IF NOT EXISTS decision_expected_date DATE NULL,
    ADD COLUMN IF NOT EXISTS next_actions VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS consultation_claim_next_actions (
    id CHAR(36) NOT NULL,
    consultation_claim_detail_id CHAR(36) NOT NULL,
    action_order INT NOT NULL,
    action_content VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by CHAR(36) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultation_claim_next_actions_detail_order (
        consultation_claim_detail_id,
        action_order
    ),
    CONSTRAINT fk_consultation_claim_next_actions_detail
        FOREIGN KEY (consultation_claim_detail_id)
        REFERENCES consultation_claim_details(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Preserve the existing event date under the current incident_date name.
UPDATE consultation_claim_details
SET incident_date = COALESCE(incident_date, claim_event_date);

-- Current request values are free-form strings, so preserve them without legacy enum checks.
ALTER TABLE consultation_claim_types
    DROP CONSTRAINT IF EXISTS chk_consultation_claim_types_type;

ALTER TABLE consultation_claim_review_items
    DROP CONSTRAINT IF EXISTS chk_consultation_claim_review_items_type;

-- Migrate columns that may have been generated locally by Hibernate.
INSERT IGNORE INTO consultation_claim_types (
    id,
    consultation_claim_detail_id,
    claim_type,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
)
SELECT
    UUID(),
    detail.id,
    detail.claim_type,
    detail.created_at,
    detail.created_by,
    detail.updated_at,
    detail.updated_by,
    detail.deleted_at,
    detail.deleted_by
FROM consultation_claim_details detail
WHERE detail.claim_type IS NOT NULL
  AND TRIM(detail.claim_type) <> '';

INSERT IGNORE INTO consultation_claim_review_items (
    id,
    consultation_claim_detail_id,
    review_type,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
)
SELECT
    UUID(),
    detail.id,
    migrated.review_type,
    detail.created_at,
    detail.created_by,
    detail.updated_at,
    detail.updated_by,
    detail.deleted_at,
    detail.deleted_by
FROM consultation_claim_details detail
JOIN JSON_TABLE(
    CASE
        WHEN JSON_VALID(detail.review_items) THEN detail.review_items
        ELSE JSON_ARRAY(detail.review_items)
    END,
    '$[*]' COLUMNS (
        review_type VARCHAR(50) PATH '$'
    )
) migrated
WHERE detail.review_items IS NOT NULL
  AND TRIM(migrated.review_type) <> '';

INSERT IGNORE INTO consultation_claim_next_actions (
    id,
    consultation_claim_detail_id,
    action_order,
    action_content,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted_at,
    deleted_by
)
SELECT
    UUID(),
    detail.id,
    migrated.action_order,
    migrated.action_content,
    detail.created_at,
    detail.created_by,
    detail.updated_at,
    detail.updated_by,
    detail.deleted_at,
    detail.deleted_by
FROM consultation_claim_details detail
JOIN JSON_TABLE(
    CASE
        WHEN JSON_VALID(detail.next_actions) THEN detail.next_actions
        ELSE JSON_ARRAY(detail.next_actions)
    END,
    '$[*]' COLUMNS (
        action_order FOR ORDINALITY,
        action_content VARCHAR(500) PATH '$'
    )
) migrated
WHERE detail.next_actions IS NOT NULL
  AND TRIM(migrated.action_content) <> '';

ALTER TABLE consultation_claim_details
    DROP COLUMN IF EXISTS claim_event_date,
    DROP COLUMN IF EXISTS guidance_summary,
    DROP COLUMN IF EXISTS claim_type,
    DROP COLUMN IF EXISTS review_items,
    DROP COLUMN IF EXISTS next_actions;

-- Restore the intended Flyway column definitions after local Hibernate updates.
ALTER TABLE consultations
    MODIFY COLUMN special_note TEXT NULL;

UPDATE consultation_ai_briefings
SET briefing_content = ''
WHERE briefing_content IS NULL;

ALTER TABLE consultation_ai_briefings
    MODIFY COLUMN briefing_content TEXT NOT NULL;

ALTER TABLE consultation_new_details
    MODIFY COLUMN monthly_income DECIMAL(15,2) NULL,
    MODIFY COLUMN monthly_insurance_premium DECIMAL(15,2) NULL;

ALTER TABLE consultation_renewal_details
    MODIFY COLUMN current_premium DECIMAL(15,2) NOT NULL,
    MODIFY COLUMN renewal_premium DECIMAL(15,2) NOT NULL,
    MODIFY COLUMN premium_change_rate DECIMAL(5,2) NOT NULL;
