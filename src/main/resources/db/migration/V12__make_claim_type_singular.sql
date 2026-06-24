ALTER TABLE consultation_claim_details
    ADD COLUMN IF NOT EXISTS claim_type VARCHAR(50) NULL AFTER incident_date;

UPDATE consultation_claim_details detail
JOIN (
    SELECT consultation_claim_detail_id, claim_type
    FROM (
        SELECT
            consultation_claim_detail_id,
            claim_type,
            ROW_NUMBER() OVER (
                PARTITION BY consultation_claim_detail_id
                ORDER BY created_at, id
            ) AS claim_type_rank
        FROM consultation_claim_types
        WHERE deleted_at IS NULL
    ) ranked_claim_types
    WHERE claim_type_rank = 1
) legacy_claim_type ON legacy_claim_type.consultation_claim_detail_id = detail.id
SET detail.claim_type = legacy_claim_type.claim_type;

DROP TABLE consultation_claim_types;
