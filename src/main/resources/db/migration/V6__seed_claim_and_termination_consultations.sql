    -- Add claim and termination consultation demo data to databases where V2 was
-- already applied before those consultation types were added to the seed.

SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';

CREATE TEMPORARY TABLE tmp_v6_consultation_contracts (
    seq_no INT NOT NULL,
    customer_id CHAR(36) NOT NULL,
    fp_id CHAR(36) NOT NULL,
    contract_id CHAR(36) NOT NULL,
    consultation_sequence INT NOT NULL,
    PRIMARY KEY (seq_no)
);

INSERT INTO tmp_v6_consultation_contracts (
    seq_no,
    customer_id,
    fp_id,
    contract_id,
    consultation_sequence
)
SELECT
    ROW_NUMBER() OVER (ORDER BY selected.contract_code) AS seq_no,
    selected.customer_id,
    selected.fp_id,
    selected.contract_id,
    COALESCE(existing.max_sequence, 0) + 1 AS consultation_sequence
FROM (
    SELECT
        ranked.customer_id,
        ranked.fp_id,
        ranked.contract_id,
        ranked.contract_code
    FROM (
        SELECT
            ct.customer_id,
            ct.fp_id,
            ct.id AS contract_id,
            ct.contract_code,
            ROW_NUMBER() OVER (
                PARTITION BY ct.customer_id
                ORDER BY ct.contract_code
            ) AS customer_contract_rank
        FROM contracts ct
        WHERE ct.contract_status = 'MAINTENANCE'
    ) ranked
    WHERE ranked.customer_contract_rank = 1
    ORDER BY ranked.contract_code
    LIMIT 200
) selected
LEFT JOIN (
    SELECT
        customer_id,
        MAX(consultation_sequence) AS max_sequence
    FROM consultations
    GROUP BY customer_id
) existing ON existing.customer_id = selected.customer_id;

INSERT INTO consultations (
    id,
    consultation_sequence,
    customer_id,
    fp_id,
    contract_id,
    consultation_type,
    consultation_channel,
    consulted_at,
    special_note,
    next_scheduled_at,
    created_by,
    updated_by
)
SELECT
    CONCAT('76000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    consultation_sequence,
    customer_id,
    fp_id,
    contract_id,
    CASE WHEN seq_no <= 100 THEN 'CLAIM' ELSE 'TERMINATION' END AS consultation_type,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'VISIT'
        WHEN 1 THEN 'PHONE'
        ELSE 'MESSAGE'
    END AS consultation_channel,
    DATE_ADD('2026-03-01 09:00:00', INTERVAL seq_no DAY) AS consulted_at,
    CASE
        WHEN seq_no <= 100 THEN CONCAT('Claim consultation demo ', LPAD(seq_no, 3, '0'))
        ELSE CONCAT('Termination consultation demo ', LPAD(seq_no - 100, 3, '0'))
    END AS special_note,
    CASE
        WHEN seq_no <= 100 AND MOD(seq_no, 4) = 0
            THEN DATE_ADD('2026-06-15 10:00:00', INTERVAL MOD(seq_no, 30) DAY)
        WHEN seq_no > 100 AND MOD(seq_no, 5) = 0
            THEN DATE_ADD('2026-06-15 14:00:00', INTERVAL MOD(seq_no, 30) DAY)
        ELSE NULL
    END AS next_scheduled_at,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM tmp_v6_consultation_contracts;

INSERT INTO consultation_claim_details (
    id,
    consultation_id,
    claim_stage,
    claim_event_date,
    claim_reason_detail,
    hospital_name,
    diagnosis_or_treatment,
    hospitalization_status,
    surgery_status,
    claim_result,
    guidance_summary,
    created_by,
    updated_by
)
SELECT
    CONCAT('76100000-0000-0000-0000-', LPAD(seed.seq_no, 12, '0')) AS id,
    seed.consultation_id,
    CASE MOD(seed.seq_no, 4)
        WHEN 0 THEN 'DOCUMENT_GUIDANCE'
        WHEN 1 THEN 'IN_REVIEW'
        WHEN 2 THEN 'SUPPLEMENT_REQUESTED'
        ELSE 'PAYMENT_CONFIRMED'
    END AS claim_stage,
    DATE_SUB(DATE(seed.consulted_at), INTERVAL MOD(seed.seq_no, 30) + 1 DAY) AS claim_event_date,
    CONCAT('Claim event and coverage review case ', LPAD(seed.seq_no, 3, '0')) AS claim_reason_detail,
    CONCAT('Relia Medical Center ', MOD(seed.seq_no, 10) + 1) AS hospital_name,
    CASE MOD(seed.seq_no, 4)
        WHEN 0 THEN 'Outpatient treatment'
        WHEN 1 THEN 'Hospitalization treatment'
        WHEN 2 THEN 'Surgery and follow-up treatment'
        ELSE 'Diagnostic examination'
    END AS diagnosis_or_treatment,
    CASE MOD(seed.seq_no, 3)
        WHEN 0 THEN 'OUTPATIENT'
        WHEN 1 THEN 'HOSPITALIZED'
        ELSE 'DISCHARGED'
    END AS hospitalization_status,
    CASE WHEN MOD(seed.seq_no, 2) = 0 THEN 'NONE' ELSE 'COMPLETED' END AS surgery_status,
    CASE MOD(seed.seq_no, 4)
        WHEN 0 THEN 'RECEIVED'
        WHEN 1 THEN 'IN_REVIEW'
        WHEN 2 THEN 'SUPPLEMENT_REQUIRED'
        ELSE 'PAID'
    END AS claim_result,
    CONCAT('Required claim documents and process explained for case ', LPAD(seed.seq_no, 3, '0')),
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        selected.seq_no,
        cs.id AS consultation_id,
        cs.consulted_at
    FROM tmp_v6_consultation_contracts selected
    JOIN consultations cs
        ON cs.id = CONCAT('76000000-0000-0000-0000-', LPAD(selected.seq_no, 12, '0'))
    WHERE selected.seq_no <= 100
) seed;

INSERT INTO consultation_claim_types (
    id,
    consultation_claim_detail_id,
    claim_type,
    created_by,
    updated_by
)
SELECT
    CONCAT('76300000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    CONCAT('76100000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS consultation_claim_detail_id,
    CASE MOD(seq_no, 6)
        WHEN 0 THEN 'ACTUAL_MEDICAL'
        WHEN 1 THEN 'HOSPITALIZATION'
        WHEN 2 THEN 'OUTPATIENT'
        WHEN 3 THEN 'SURGERY'
        WHEN 4 THEN 'DIAGNOSIS'
        ELSE 'INJURY'
    END AS claim_type,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM tmp_v6_consultation_contracts
WHERE seq_no <= 100;

INSERT INTO consultation_claim_review_items (
    id,
    consultation_claim_detail_id,
    review_type,
    created_by,
    updated_by
)
SELECT
    CONCAT('76400000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    CONCAT('76100000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS consultation_claim_detail_id,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN 'COVERAGE_ELIGIBLE'
        WHEN 1 THEN 'EXEMPTION_PERIOD'
        WHEN 2 THEN 'EXCLUSION_POSSIBILITY'
        ELSE 'PREVIOUS_CLAIM_HISTORY'
    END AS review_type,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM tmp_v6_consultation_contracts
WHERE seq_no <= 100;

INSERT INTO consultation_cancel_details (
    id,
    consultation_id,
    premium_burden,
    renewal_premium_burden,
    payment_difficulty,
    coverage_dissatisfaction,
    duplicate_insurance,
    product_remodeling_review,
    comparing_other_company,
    moving_to_other_company,
    planner_contact_dissatisfaction,
    management_dissatisfaction,
    retention_possibility,
    created_by,
    updated_by
)
SELECT
    CONCAT('76200000-0000-0000-0000-', LPAD(seq_no - 100, 12, '0')) AS id,
    CONCAT('76000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS consultation_id,
    MOD(seq_no, 2) = 0 AS premium_burden,
    MOD(seq_no, 3) = 0 AS renewal_premium_burden,
    MOD(seq_no, 5) = 0 AS payment_difficulty,
    MOD(seq_no, 4) = 0 AS coverage_dissatisfaction,
    MOD(seq_no, 6) = 0 AS duplicate_insurance,
    MOD(seq_no, 3) IN (1, 2) AS product_remodeling_review,
    MOD(seq_no, 7) = 0 AS comparing_other_company,
    MOD(seq_no, 8) = 0 AS moving_to_other_company,
    MOD(seq_no, 9) = 0 AS planner_contact_dissatisfaction,
    MOD(seq_no, 10) = 0 AS management_dissatisfaction,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'HIGH'
        WHEN 1 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS retention_possibility,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM tmp_v6_consultation_contracts
WHERE seq_no > 100;

DROP TEMPORARY TABLE tmp_v6_consultation_contracts;
