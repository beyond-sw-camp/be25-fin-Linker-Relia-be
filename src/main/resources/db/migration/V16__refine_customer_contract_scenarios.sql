SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';
SET @REFERENCE_DATE = DATE('2026-06-24');

UPDATE customers
SET customer_name = CONCAT(
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 16) + 1, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '신', '권', '송', '류'),
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 3, 16) + 1, '민', '서', '지', '도', '하', '주', '유', '다', '현', '정', '재', '수', '소', '나', '지', '채'),
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 7, 16) + 1, '준', '연', '원', '윤', '아', '호', '림', '진', '현', '우', '영', '경', '빈', '솔', '율', '희')
    ),
    customer_job = ELT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 10) + 1,
        '회사원', '교사', '간호사', '개발자', '회계사', '자영업', '공무원', '디자이너', '영업관리자', '연구원'
    ),
    customer_company_name = ELT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 5, 10) + 1,
        '리리아파트너스', '서울교육지원청', '메디플러스병원', '넥스트코어', '알파파이낸스', '해솔유통', '한강모빌리티', '다온커머스', '미래에셋서비스', '오름테크'
    ),
    customer_address_detail = CONCAT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 22) + 2,
        '층 ',
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 30) + 101,
        '호'
    ),
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE customer_code LIKE 'CUS-%';

UPDATE contracts ct
JOIN insurance_products ip ON ip.id = ct.insurance_product_id
JOIN insurance_categories ic ON ic.id = ip.insurance_category_id
SET ct.payment_period_years = CASE ic.insurance_category_code
        WHEN 'CAT001' THEN 10
        WHEN 'CAT002' THEN 15
        WHEN 'CAT003' THEN 10
        WHEN 'CAT004' THEN 10
        WHEN 'CAT005' THEN 15
        ELSE 10
    END,
    ct.monthly_premium = CASE ic.insurance_category_code
        WHEN 'CAT001' THEN 70000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 8) * 12000)
        WHEN 'CAT002' THEN 110000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 10) * 15000)
        WHEN 'CAT003' THEN 35000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 8) * 7000)
        WHEN 'CAT004' THEN 18000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 7) * 5000)
        WHEN 'CAT005' THEN 90000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 8) * 14000)
        ELSE 250000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 6) * 40000)
    END,
    ct.contract_date = DATE_ADD('2020-01-15', INTERVAL MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 2150) DAY),
    ct.contract_start_date = DATE_ADD('2020-01-15', INTERVAL MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 2150) DAY),
    ct.coverage_start_date = DATE_ADD('2020-01-15', INTERVAL MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 2150) DAY),
    ct.contract_status = 'MAINTENANCE',
    ct.coverage_summary = CONCAT('고객 맞춤 보장 설계안 ', RIGHT(ct.contract_code, 6)),
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID
WHERE ct.contract_code LIKE 'CTR%';

UPDATE contracts
SET contract_end_date = LEAST(
        DATE('2039-12-31'),
        DATE_ADD(contract_start_date, INTERVAL payment_period_years YEAR)
    ),
    coverage_end_date = LEAST(
        DATE('2039-12-31'),
        DATE_ADD(coverage_start_date, INTERVAL payment_period_years YEAR)
    ),
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE contract_code LIKE 'CTR%';

DROP TEMPORARY TABLE IF EXISTS tmp_v16_recent_contract_targets;
CREATE TEMPORARY TABLE tmp_v16_recent_contract_targets (
    seq_no INT NOT NULL,
    contract_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    scenario_contract_date DATE NOT NULL,
    scenario_customer_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    PRIMARY KEY (seq_no),
    UNIQUE KEY uk_tmp_v16_recent_contract_targets_contract (contract_id)
);

INSERT INTO tmp_v16_recent_contract_targets (
    seq_no,
    contract_id,
    scenario_contract_date
)
    SELECT
        target.seq_no,
        target.contract_id,
        DATE_ADD(
            CASE
                WHEN target.seq_no <= 6 THEN DATE('2025-12-05')
                WHEN target.seq_no <= 18 THEN DATE('2026-01-05')
                WHEN target.seq_no <= 42 THEN DATE('2026-02-05')
                WHEN target.seq_no <= 70 THEN DATE('2026-03-05')
                WHEN target.seq_no <= 84 THEN DATE('2026-04-05')
                ELSE DATE('2026-05-05')
            END,
            INTERVAL MOD(target.seq_no - 1, 10) * 2 DAY
        ) AS scenario_contract_date
FROM (
    SELECT
        ROW_NUMBER() OVER (
            ORDER BY ranked.sort_key_1, ranked.sort_key_2, ranked.contract_code
        ) AS seq_no,
        ranked.contract_id
    FROM (
        SELECT
            ct.id AS contract_id,
            ct.contract_code,
            MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 61, 257) AS sort_key_1,
            MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 127) AS sort_key_2
        FROM contracts ct
        WHERE ct.contract_code LIKE 'CTR%'
    ) ranked
    LIMIT 96
) target;

UPDATE tmp_v16_recent_contract_targets recent_targets
JOIN (
    SELECT
        ranked.seq_no,
        ranked.customer_id
    FROM (
        SELECT
            ROW_NUMBER() OVER (
                ORDER BY MOD(CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED) * 43, 257),
                         MOD(CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED) * 17, 131),
                         c.customer_code
            ) AS seq_no,
            c.id AS customer_id
        FROM customers c
        WHERE c.customer_code LIKE 'CUS-%'
          AND NOT EXISTS (
              SELECT 1
              FROM contracts ct
              WHERE ct.customer_id = c.id
          )
    ) ranked
    WHERE ranked.seq_no <= 96
) prospect_targets ON prospect_targets.seq_no = recent_targets.seq_no
SET recent_targets.scenario_customer_id = prospect_targets.customer_id;

UPDATE contracts ct
JOIN tmp_v16_recent_contract_targets recent_targets ON recent_targets.contract_id = ct.id
SET ct.customer_id = COALESCE(recent_targets.scenario_customer_id, ct.customer_id),
    ct.contract_date = recent_targets.scenario_contract_date,
    ct.contract_start_date = recent_targets.scenario_contract_date,
    ct.coverage_start_date = recent_targets.scenario_contract_date,
    ct.payment_period_years = 10,
    ct.monthly_premium = 120000 + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 8) * 20000),
    ct.contract_end_date = DATE_ADD(recent_targets.scenario_contract_date, INTERVAL 10 YEAR),
    ct.coverage_end_date = DATE_ADD(recent_targets.scenario_contract_date, INTERVAL 10 YEAR),
    ct.contract_status = 'MAINTENANCE',
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE customers c
JOIN tmp_v16_recent_contract_targets recent_targets ON recent_targets.scenario_customer_id = c.id
SET c.created_at = LEAST(
        c.created_at,
        TIMESTAMP(
            DATE_SUB(recent_targets.scenario_contract_date, INTERVAL 14 + MOD(recent_targets.seq_no, 12) DAY),
            '09:00:00'
        )
    ),
    c.updated_at = CURRENT_TIMESTAMP,
    c.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_ADD(@REFERENCE_DATE, INTERVAL target.seq_no + 2 DAY) AS due_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        JOIN insurance_products ip ON ip.id = ct.insurance_product_id
        WHERE ip.is_renewable = TRUE
          AND ct.contract_status = 'MAINTENANCE'
          AND ct.contract_code LIKE 'CTR%'
          AND NOT EXISTS (
              SELECT 1
              FROM tmp_v16_recent_contract_targets recent_targets
              WHERE recent_targets.contract_id = ct.id
          )
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 211),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 29, 97),
                 ct.contract_code
        LIMIT 27
    ) target
) renewal_due_targets ON renewal_due_targets.contract_id = ct.id
SET ct.contract_end_date = renewal_due_targets.due_date,
    ct.coverage_end_date = renewal_due_targets.due_date,
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_ADD(@REFERENCE_DATE, INTERVAL target.seq_no + 11 DAY) AS due_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        JOIN insurance_products ip ON ip.id = ct.insurance_product_id
        WHERE ip.is_renewable = FALSE
          AND ct.contract_status = 'MAINTENANCE'
          AND ct.contract_code LIKE 'CTR%'
          AND NOT EXISTS (
              SELECT 1
              FROM tmp_v16_recent_contract_targets recent_targets
              WHERE recent_targets.contract_id = ct.id
          )
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 23, 223),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 31, 89),
                 ct.contract_code
        LIMIT 21
    ) target
) maturity_due_targets ON maturity_due_targets.contract_id = ct.id
SET ct.contract_end_date = maturity_due_targets.due_date,
    ct.coverage_end_date = maturity_due_targets.due_date,
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_SUB(@REFERENCE_DATE, INTERVAL target.seq_no + 18 DAY) AS completed_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        WHERE ct.contract_status = 'MAINTENANCE'
          AND ct.contract_code LIKE 'CTR%'
          AND NOT EXISTS (
              SELECT 1
              FROM tmp_v16_recent_contract_targets recent_targets
              WHERE recent_targets.contract_id = ct.id
          )
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 43, 239),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 11, 107),
                 ct.contract_code
        LIMIT 78
    ) target
) completed_targets ON completed_targets.contract_id = ct.id
SET ct.contract_end_date = completed_targets.completed_date,
    ct.coverage_end_date = completed_targets.completed_date,
    ct.contract_status = 'COMPLETED',
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_SUB(@REFERENCE_DATE, INTERVAL target.seq_no + 9 DAY) AS terminated_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code DESC) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        WHERE ct.contract_status = 'MAINTENANCE'
          AND ct.contract_code LIKE 'CTR%'
          AND NOT EXISTS (
              SELECT 1
              FROM tmp_v16_recent_contract_targets recent_targets
              WHERE recent_targets.contract_id = ct.id
          )
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 47, 241) DESC,
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 13, 109) DESC,
                 ct.contract_code DESC
        LIMIT 45
    ) target
) terminated_targets ON terminated_targets.contract_id = ct.id
SET ct.contract_end_date = terminated_targets.terminated_date,
    ct.coverage_end_date = terminated_targets.terminated_date,
    ct.contract_status = 'TERMINATED',
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code DESC) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        WHERE ct.contract_status = 'MAINTENANCE'
          AND ct.contract_code LIKE 'CTR%'
          AND NOT EXISTS (
              SELECT 1
              FROM tmp_v16_recent_contract_targets recent_targets
              WHERE recent_targets.contract_id = ct.id
          )
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 59, 251) DESC,
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 113) DESC,
                 ct.contract_code DESC
        LIMIT 28
    ) target
) lapsed_targets ON lapsed_targets.contract_id = ct.id
SET ct.contract_status = 'LAPSED',
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID;

DELETE FROM branch_income_commission_monthly_closing;
DELETE FROM income_commission_monthly_closing;
DELETE FROM branch_commission_monthly_closing;
DELETE FROM fp_commission_monthly_closing;
DELETE FROM payment_commission_records;
DELETE FROM gross_commission_records;
DELETE FROM contract_monthly_closing;
DELETE FROM fp_monthly_info;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_months_v16;
CREATE TEMPORARY TABLE tmp_demo_months_v16 (
    month_seq INT NOT NULL,
    closing_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    closing_date DATE NOT NULL,
    PRIMARY KEY (month_seq)
);

INSERT INTO tmp_demo_months_v16 (month_seq, closing_month, closing_date)
VALUES
    (1, '2025-12', DATE('2025-12-31')),
    (2, '2026-01', DATE('2026-01-31')),
    (3, '2026-02', DATE('2026-02-28')),
    (4, '2026-03', DATE('2026-03-31')),
    (5, '2026-04', DATE('2026-04-30')),
    (6, '2026-05', DATE('2026-05-31'));

INSERT INTO contract_monthly_closing (
    id,
    closing_month,
    contract_id,
    contract_status,
    payment_status,
    current_payment_round,
    maintenance_round,
    lapse_yn,
    lapse_at,
    terminated_yn,
    terminated_at,
    customer_id,
    fp_id,
    contract_date,
    contract_start_date,
    contract_end_date,
    payment_period_years,
    payment_cycle,
    monthly_premium,
    coverage_start_date,
    coverage_end_date,
    coverage_summary,
    closed_at
)
SELECT
    CONCAT('62500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY closing_seed.closing_month, closing_seed.contract_code), 12, '0')) AS id,
    closing_seed.closing_month,
    closing_seed.contract_id,
    closing_seed.snapshot_contract_status,
    closing_seed.payment_status,
    closing_seed.scheduled_payment_round,
    CASE
        WHEN closing_seed.snapshot_contract_status IN ('MAINTENANCE', 'LAPSED') THEN GREATEST(
            0,
            closing_seed.scheduled_payment_round - closing_seed.unpaid_progress_count
        )
        ELSE NULL
    END AS maintenance_round,
    CASE WHEN closing_seed.snapshot_contract_status = 'LAPSED' THEN TRUE ELSE FALSE END AS lapse_yn,
    CASE WHEN closing_seed.snapshot_contract_status = 'LAPSED' THEN closing_seed.lapse_at ELSE NULL END AS lapse_at,
    CASE WHEN closing_seed.snapshot_contract_status = 'TERMINATED' THEN TRUE ELSE FALSE END AS terminated_yn,
    CASE WHEN closing_seed.snapshot_contract_status = 'TERMINATED' THEN closing_seed.terminated_at ELSE NULL END AS terminated_at,
    closing_seed.customer_id,
    closing_seed.fp_id,
    closing_seed.contract_date,
    closing_seed.contract_start_date,
    closing_seed.contract_end_date,
    closing_seed.payment_period_years,
    closing_seed.payment_cycle,
    closing_seed.monthly_premium,
    closing_seed.coverage_start_date,
    closing_seed.coverage_end_date,
    closing_seed.coverage_summary,
    TIMESTAMP(closing_seed.closing_date, '18:00:00') AS closed_at
FROM (
    SELECT
        base_seed.*,
        CASE
            WHEN base_seed.unpaid_installment_count IS NOT NULL
                 AND base_seed.month_seq >= 7 - base_seed.unpaid_installment_count
                 AND (
                     base_seed.snapshot_contract_status = 'MAINTENANCE'
                     OR (base_seed.snapshot_contract_status = 'LAPSED' AND base_seed.month_seq = 6)
                 )
                THEN 'UNPAID'
            ELSE 'PAID'
        END AS payment_status,
        CASE
            WHEN base_seed.unpaid_installment_count IS NOT NULL
                 AND base_seed.month_seq >= 7 - base_seed.unpaid_installment_count
                 AND (
                     base_seed.snapshot_contract_status = 'MAINTENANCE'
                     OR (base_seed.snapshot_contract_status = 'LAPSED' AND base_seed.month_seq = 6)
                 )
                THEN LEAST(
                    base_seed.unpaid_installment_count,
                    base_seed.month_seq - (7 - base_seed.unpaid_installment_count) + 1
                )
            ELSE 0
        END AS unpaid_progress_count
    FROM (
        SELECT
            months.month_seq,
            months.closing_month,
            months.closing_date,
            ct.contract_code,
            ct.id AS contract_id,
            CASE
                WHEN terminated_targets.event_month_seq IS NOT NULL
                     AND months.month_seq >= terminated_targets.event_month_seq THEN 'TERMINATED'
                WHEN lapsed_targets.event_month_seq IS NOT NULL
                     AND months.month_seq >= lapsed_targets.event_month_seq THEN 'LAPSED'
                WHEN ct.contract_end_date < months.closing_date THEN 'COMPLETED'
                ELSE 'MAINTENANCE'
            END AS snapshot_contract_status,
            LEAST(
                ct.payment_period_years * 12,
                GREATEST(1, TIMESTAMPDIFF(MONTH, ct.contract_start_date, months.closing_date) + 1)
            ) AS scheduled_payment_round,
            unpaid_targets.unpaid_installment_count,
            CASE
                WHEN lapsed_targets.event_month_seq IS NOT NULL
                     AND months.month_seq >= lapsed_targets.event_month_seq THEN lapsed_targets.event_date
                ELSE NULL
            END AS lapse_at,
            CASE
                WHEN terminated_targets.event_month_seq IS NOT NULL
                     AND months.month_seq >= terminated_targets.event_month_seq THEN terminated_targets.event_date
                ELSE NULL
            END AS terminated_at,
            ct.customer_id,
            ct.fp_id,
            ct.contract_date,
            ct.contract_start_date,
            ct.contract_end_date,
            ct.payment_period_years,
            ct.payment_cycle,
            ct.monthly_premium,
            ct.coverage_start_date,
            ct.coverage_end_date,
            ct.coverage_summary
        FROM contracts ct
        JOIN tmp_demo_months_v16 months
            ON ct.contract_start_date <= months.closing_date
        LEFT JOIN (
            SELECT
                target.contract_id,
                target.unpaid_installment_count
            FROM (
                SELECT
                    maintenance_targets.contract_id,
                    maintenance_targets.unpaid_installment_count
                FROM contracts ct
                JOIN (
                    SELECT
                        ranked.contract_id,
                        CASE
                            WHEN ranked.seq_no <= 14 THEN 1
                            ELSE 2
                        END AS unpaid_installment_count
                    FROM (
                        SELECT
                            ROW_NUMBER() OVER (
                                ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 71, 257),
                                         MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 37, 127),
                                         ct.contract_code
                            ) AS seq_no,
                            ct.id AS contract_id
                        FROM contracts ct
                        WHERE ct.contract_status = 'MAINTENANCE'
                          AND ct.contract_code LIKE 'CTR%'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM tmp_v16_recent_contract_targets recent_targets
                              WHERE recent_targets.contract_id = ct.id
                          )
                    ) ranked
                    WHERE ranked.seq_no <= 24
                ) maintenance_targets ON maintenance_targets.contract_id = ct.id
                WHERE ct.contract_status = 'MAINTENANCE'
                  AND ct.contract_code LIKE 'CTR%'

                UNION ALL

                SELECT
                    ct.id AS contract_id,
                    3 AS unpaid_installment_count
                FROM contracts ct
                WHERE ct.contract_status = 'LAPSED'
                  AND ct.contract_code LIKE 'CTR%'
            ) target
        ) unpaid_targets ON unpaid_targets.contract_id = ct.id
        LEFT JOIN (
            SELECT
                target.contract_id,
                6 AS event_month_seq,
                DATE('2026-05-26') AS event_date
            FROM (
                SELECT
                    ct.id AS contract_id
                FROM contracts ct
                WHERE ct.contract_status = 'LAPSED'
                  AND ct.contract_code LIKE 'CTR%'
            ) target
        ) lapsed_targets ON lapsed_targets.contract_id = ct.id
        LEFT JOIN (
            SELECT
                target.contract_id,
                MOD(target.seq_no - 1, 5) + 2 AS event_month_seq,
                CASE MOD(target.seq_no - 1, 5)
                    WHEN 0 THEN DATE('2026-01-27')
                    WHEN 1 THEN DATE('2026-02-27')
                    WHEN 2 THEN DATE('2026-03-27')
                    WHEN 3 THEN DATE('2026-04-27')
                    ELSE DATE('2026-05-27')
                END AS event_date
            FROM (
                SELECT
                    ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
                    ct.id AS contract_id
                FROM contracts ct
                WHERE ct.contract_status = 'TERMINATED'
                  AND ct.contract_code LIKE 'CTR%'
            ) target
        ) terminated_targets ON terminated_targets.contract_id = ct.id
        WHERE ct.contract_code LIKE 'CTR%'
    ) base_seed
) closing_seed;

UPDATE customers c
SET c.customer_status = CASE
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
              AND ct.contract_status = 'MAINTENANCE'
        ) THEN 'CONTRACTED'
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
        ) THEN 'CLOSED'
        ELSE 'PROSPECT'
    END,
    c.updated_at = CURRENT_TIMESTAMP,
    c.updated_by = @SYSTEM_USER_ID;

UPDATE customers c
LEFT JOIN (
    SELECT
        ct.customer_id,
        MIN(
            CASE
                WHEN cmc.payment_status = 'UNPAID' THEN 1
                WHEN ip.is_renewable = TRUE
                     AND ct.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN @REFERENCE_DATE AND DATE_ADD(@REFERENCE_DATE, INTERVAL 30 DAY) THEN 2
                WHEN ip.is_renewable = FALSE
                     AND ct.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN @REFERENCE_DATE AND DATE_ADD(@REFERENCE_DATE, INTERVAL 30 DAY) THEN 3
                ELSE 9
            END
        ) AS interest_priority
    FROM contracts ct
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    LEFT JOIN contract_monthly_closing cmc
        ON cmc.contract_id = ct.id
       AND cmc.closing_month = '2026-05'
    GROUP BY ct.customer_id
) interest_targets ON interest_targets.customer_id = c.id
SET c.interest_yn = CASE
        WHEN c.customer_status = 'CONTRACTED' AND interest_targets.interest_priority < 9 THEN TRUE
        ELSE FALSE
    END,
    c.interest_reason = CASE
        WHEN c.customer_status <> 'CONTRACTED' THEN NULL
        WHEN interest_targets.interest_priority = 1 THEN 'UNPAID'
        WHEN interest_targets.interest_priority = 2 THEN 'RENEWAL_DUE'
        WHEN interest_targets.interest_priority = 3 THEN 'MATURITY_DUE'
        ELSE NULL
    END,
    c.updated_at = CURRENT_TIMESTAMP,
    c.updated_by = @SYSTEM_USER_ID;

DELETE FROM customer_status_history;

INSERT INTO customer_status_history (
    id,
    customer_status_sequence,
    customer_id,
    before_status,
    after_status,
    changed_reason,
    changed_at,
    changed_by
)
SELECT
    CONCAT('83500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY c.customer_code), 12, '0')),
    1,
    c.id,
    NULL,
    'PROSPECT',
    '신규 고객 생성',
    c.created_at,
    COALESCE(c.created_by, @SYSTEM_USER_ID)
FROM customers c;

INSERT INTO customer_status_history (
    id,
    customer_status_sequence,
    customer_id,
    before_status,
    after_status,
    changed_reason,
    changed_at,
    changed_by
)
SELECT
    CONCAT('83600000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY seeded.customer_code), 12, '0')),
    2,
    seeded.customer_id,
    'PROSPECT',
    'CONTRACTED',
    '계약 등록',
    seeded.first_contract_date,
    @SYSTEM_USER_ID
FROM (
    SELECT
        c.customer_code,
        c.id AS customer_id,
        MIN(ct.contract_date) AS first_contract_date
    FROM customers c
    JOIN contracts ct ON ct.customer_id = c.id
    GROUP BY c.customer_code, c.id
) seeded;

INSERT INTO customer_status_history (
    id,
    customer_status_sequence,
    customer_id,
    before_status,
    after_status,
    changed_reason,
    changed_at,
    changed_by
)
SELECT
    CONCAT('83700000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY closed_seed.customer_code), 12, '0')),
    3,
    closed_seed.customer_id,
    'CONTRACTED',
    'CLOSED',
    '계약 종료 반영',
    closed_seed.closed_at,
    @SYSTEM_USER_ID
FROM (
    SELECT
        c.customer_code,
        c.id AS customer_id,
        MAX(
            CASE
                WHEN cmc.contract_status = 'LAPSED' AND cmc.lapse_at IS NOT NULL THEN cmc.lapse_at
                WHEN cmc.contract_status = 'TERMINATED' AND cmc.terminated_at IS NOT NULL THEN cmc.terminated_at
                WHEN cmc.contract_status = 'COMPLETED' THEN cmc.contract_end_date
                ELSE NULL
            END
        ) AS closed_at
    FROM customers c
    JOIN contract_monthly_closing cmc ON cmc.customer_id = c.id
    WHERE c.customer_status = 'CLOSED'
    GROUP BY c.customer_code, c.id
) closed_seed
WHERE closed_seed.closed_at IS NOT NULL;

UPDATE consultations cs
JOIN customers c ON c.id = cs.customer_id
SET cs.next_scheduled_at = CASE
        WHEN c.customer_status = 'CLOSED' THEN NULL
        WHEN cs.consultation_type = 'TERMINATION' THEN NULL
        WHEN DATE(cs.consulted_at) < DATE_SUB(@REFERENCE_DATE, INTERVAL 35 DAY) THEN DATE_ADD(cs.consulted_at, INTERVAL 14 DAY)
        ELSE cs.next_scheduled_at
    END,
    cs.updated_at = CURRENT_TIMESTAMP,
    cs.updated_by = @SYSTEM_USER_ID;

INSERT INTO gross_commission_records (
    id,
    commission_month,
    contract_id,
    insurance_company_id,
    insurance_product_id,
    commission_type,
    gross_commission_amount,
    created_by
)
SELECT
    CONCAT('94500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY src.sort_order, src.contract_code, src.commission_month), 12, '0')),
    src.commission_month,
    src.contract_id,
    src.insurance_company_id,
    src.insurance_product_id,
    src.commission_type,
    src.gross_commission_amount,
    @SYSTEM_USER_ID
FROM (
    SELECT
        1 AS sort_order,
        ct.contract_code,
        cmc.contract_id,
        ip.insurance_company_id,
        ct.insurance_product_id,
        cmc.closing_month AS commission_month,
        CASE
            WHEN DATE_FORMAT(ct.contract_date, '%Y-%m') = cmc.closing_month THEN 'INITIAL'
            ELSE 'MAINTENANCE'
        END AS commission_type,
        ROUND(
            cmc.monthly_premium * CASE
                WHEN DATE_FORMAT(ct.contract_date, '%Y-%m') = cmc.closing_month THEN 10
                ELSE 2
            END,
            2
        ) AS gross_commission_amount
    FROM contract_monthly_closing cmc
    JOIN contracts ct ON ct.id = cmc.contract_id
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    WHERE cmc.contract_status = 'MAINTENANCE'
      AND cmc.payment_status = 'PAID'
      AND ct.contract_code LIKE 'CTR%'

    UNION ALL

    SELECT
        2 AS sort_order,
        ct.contract_code,
        ct.id AS contract_id,
        ip.insurance_company_id,
        ct.insurance_product_id,
        cmc.closing_month AS commission_month,
        'RECOVERY' AS commission_type,
        ROUND(COALESCE((
            SELECT SUM(
                prev_cmc.monthly_premium * CASE
                    WHEN DATE_FORMAT(ct.contract_date, '%Y-%m') = prev_cmc.closing_month THEN 10
                    ELSE 2
                END
            )
            FROM contract_monthly_closing prev_cmc
            WHERE prev_cmc.contract_id = ct.id
              AND prev_cmc.payment_status = 'PAID'
              AND prev_cmc.contract_status = 'MAINTENANCE'
              AND prev_cmc.closing_month < cmc.closing_month
        ), 0), 2) AS gross_commission_amount
    FROM contract_monthly_closing cmc
    JOIN contracts ct ON ct.id = cmc.contract_id
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    WHERE (
            cmc.contract_status = 'LAPSED'
        AND cmc.lapse_yn = TRUE
        AND DATE_FORMAT(cmc.lapse_at, '%Y-%m') = cmc.closing_month
    ) OR (
            cmc.contract_status = 'TERMINATED'
        AND cmc.terminated_yn = TRUE
        AND DATE_FORMAT(cmc.terminated_at, '%Y-%m') = cmc.closing_month
    )
) src
WHERE src.gross_commission_amount > 0;

INSERT INTO payment_commission_records (
    id,
    commission_month,
    gross_commission_id,
    contract_id,
    fp_id,
    organization_id,
    insurance_company_id,
    insurance_product_id,
    commission_type,
    base_commission_amount,
    fp_payment_rate,
    commission_amount,
    created_by
)
SELECT
    CONCAT('94600000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gcr.id), 12, '0')),
    gcr.commission_month,
    gcr.id,
    gcr.contract_id,
    ct.fp_id,
    u.organization_id,
    gcr.insurance_company_id,
    gcr.insurance_product_id,
    CASE gcr.commission_type
        WHEN 'INITIAL' THEN 'INITIAL_PAYMENT'
        WHEN 'MAINTENANCE' THEN 'MAINTENANCE_PAYMENT'
        ELSE 'RECOVERY_COLLECTION'
    END,
    gcr.gross_commission_amount,
    70.00,
    ROUND(gcr.gross_commission_amount * 0.7, 2),
    @SYSTEM_USER_ID
FROM gross_commission_records gcr
JOIN contracts ct ON ct.id = gcr.contract_id
JOIN users u ON u.id = ct.fp_id;

INSERT INTO fp_commission_monthly_closing (
    id,
    closing_month,
    fp_id,
    organization_id,
    total_initial_payment_amount,
    total_maintenance_payment_amount,
    total_recovery_collection_amount,
    total_payment_amount,
    net_commission_amount,
    contract_count,
    recovery_contract_count,
    closed_at
)
SELECT
    CONCAT('94700000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY pcr.commission_month, pcr.fp_id), 12, '0')),
    pcr.commission_month,
    pcr.fp_id,
    pcr.organization_id,
    ROUND(SUM(CASE WHEN pcr.commission_type = 'INITIAL_PAYMENT' THEN pcr.commission_amount ELSE 0 END), 2),
    ROUND(SUM(CASE WHEN pcr.commission_type = 'MAINTENANCE_PAYMENT' THEN pcr.commission_amount ELSE 0 END), 2),
    ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2),
    ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2),
    ROUND(
        SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END)
        - SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END),
        2
    ),
    COUNT(DISTINCT pcr.contract_id),
    COUNT(DISTINCT CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.contract_id END),
    TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(pcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
FROM payment_commission_records pcr
GROUP BY pcr.commission_month, pcr.fp_id, pcr.organization_id;

INSERT INTO branch_commission_monthly_closing (
    id,
    closing_month,
    organization_id,
    total_initial_payment_amount,
    total_maintenance_payment_amount,
    total_recovery_collection_amount,
    total_payment_amount,
    net_commission_amount,
    fp_count,
    contract_count,
    recovery_contract_count,
    closed_at
)
SELECT
    CONCAT('94800000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY closing_month, organization_id), 12, '0')),
    closing_month,
    organization_id,
    ROUND(SUM(total_initial_payment_amount), 2),
    ROUND(SUM(total_maintenance_payment_amount), 2),
    ROUND(SUM(total_recovery_collection_amount), 2),
    ROUND(SUM(total_payment_amount), 2),
    ROUND(SUM(net_commission_amount), 2),
    COUNT(DISTINCT fp_id),
    SUM(contract_count),
    SUM(recovery_contract_count),
    MAX(closed_at)
FROM fp_commission_monthly_closing
GROUP BY closing_month, organization_id;

INSERT INTO income_commission_monthly_closing (
    id,
    closing_month,
    net_income_commission_amount,
    total_initial_gross_commission_amount,
    total_maintenance_gross_commission_amount,
    total_payment_commission_amount,
    total_insurance_recovery_amount,
    total_fp_recovery_collection_amount,
    closed_at
)
SELECT
    CONCAT('94900000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gross_summary.closing_month), 12, '0')),
    gross_summary.closing_month,
    ROUND(
        gross_summary.total_initial_gross_commission_amount
        + gross_summary.total_maintenance_gross_commission_amount
        - payment_summary.total_payment_commission_amount
        - gross_summary.total_insurance_recovery_amount
        + payment_summary.total_fp_recovery_collection_amount,
        2
    ),
    gross_summary.total_initial_gross_commission_amount,
    gross_summary.total_maintenance_gross_commission_amount,
    payment_summary.total_payment_commission_amount,
    gross_summary.total_insurance_recovery_amount,
    payment_summary.total_fp_recovery_collection_amount,
    gross_summary.closed_at
FROM (
    SELECT
        gcr.commission_month AS closing_month,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
        TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(gcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00') AS closed_at
    FROM gross_commission_records gcr
    GROUP BY gcr.commission_month
) gross_summary
JOIN (
    SELECT
        pcr.commission_month AS closing_month,
        ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_commission_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_fp_recovery_collection_amount
    FROM payment_commission_records pcr
    GROUP BY pcr.commission_month
) payment_summary
    ON payment_summary.closing_month = gross_summary.closing_month;

INSERT INTO branch_income_commission_monthly_closing (
    id,
    closing_month,
    organization_id,
    net_income_commission_amount,
    total_initial_gross_commission_amount,
    total_maintenance_gross_commission_amount,
    total_gross_commission_amount,
    total_payment_commission_amount,
    total_insurance_recovery_amount,
    total_fp_recovery_collection_amount,
    contract_count,
    fp_count,
    closed_at
)
SELECT
    CONCAT('95000000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gross_summary.closing_month, gross_summary.organization_id), 12, '0')),
    gross_summary.closing_month,
    gross_summary.organization_id,
    ROUND(
        gross_summary.total_initial_gross_commission_amount
        + gross_summary.total_maintenance_gross_commission_amount
        - payment_summary.total_payment_commission_amount
        - gross_summary.total_insurance_recovery_amount
        + payment_summary.total_fp_recovery_collection_amount,
        2
    ),
    gross_summary.total_initial_gross_commission_amount,
    gross_summary.total_maintenance_gross_commission_amount,
    gross_summary.total_gross_commission_amount,
    payment_summary.total_payment_commission_amount,
    gross_summary.total_insurance_recovery_amount,
    payment_summary.total_fp_recovery_collection_amount,
    gross_summary.contract_count,
    gross_summary.fp_count,
    gross_summary.closed_at
FROM (
    SELECT
        gcr.commission_month AS closing_month,
        u.organization_id,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type IN ('INITIAL', 'MAINTENANCE') THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_gross_commission_amount,
        COUNT(DISTINCT gcr.contract_id) AS contract_count,
        COUNT(DISTINCT ct.fp_id) AS fp_count,
        TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(gcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00') AS closed_at
    FROM gross_commission_records gcr
    JOIN contracts ct ON ct.id = gcr.contract_id
    JOIN users u ON u.id = ct.fp_id
    GROUP BY gcr.commission_month, u.organization_id
) gross_summary
JOIN (
    SELECT
        pcr.commission_month AS closing_month,
        pcr.organization_id,
        ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_commission_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_fp_recovery_collection_amount
    FROM payment_commission_records pcr
    GROUP BY pcr.commission_month, pcr.organization_id
) payment_summary
    ON payment_summary.closing_month = gross_summary.closing_month
   AND payment_summary.organization_id = gross_summary.organization_id;

INSERT INTO fp_monthly_info (
    id,
    closing_month,
    emp_code,
    fp_name,
    organization_code,
    organization_name,
    organization_type,
    career_years,
    specialty_category,
    preferred_customer_age,
    preferred_customer_asset_level,
    consultation_channel,
    current_contract_count,
    retention_rate,
    consultation_count,
    handover_success_count,
    long_term_maintenance_rate,
    created_at,
    created_by
)
SELECT
    CONCAT('95100000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY m.closing_month, u.emp_code), 12, '0')),
    m.closing_month,
    u.emp_code,
    u.user_name,
    o.organization_code,
    o.organization_name,
    o.organization_type,
    GREATEST(1, TIMESTAMPDIFF(YEAR, u.joined_at, m.closing_date)),
    ELT(MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 6) + 1, '건강보험', '종신보험', '연금보험', '실손보험', '가족보장', '기업보장'),
    32 + MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 16),
    ELT(MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 3) + 1, 'LOW', 'MIDDLE', 'HIGH'),
    COALESCE(channel_summary.consultation_channel, 'PHONE'),
    COALESCE(contract_summary.current_contract_count, 0),
    COALESCE(contract_summary.retention_rate, 0.00),
    COALESCE(consultation_summary.consultation_count, 0),
    COALESCE(handover_summary.handover_success_count, 0),
    COALESCE(contract_summary.long_term_maintenance_rate, 0.00),
    TIMESTAMP(m.closing_date, '18:00:00'),
    @SYSTEM_USER_ID
FROM users u
JOIN organizations o ON o.id = u.organization_id
JOIN tmp_demo_months_v16 m
LEFT JOIN (
    SELECT
        cmc.closing_month,
        cmc.fp_id,
        COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 END) AS current_contract_count,
        ROUND(
            COALESCE(COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0), 0),
            2
        ) AS retention_rate,
        ROUND(
            COALESCE(
                COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' AND cmc.current_payment_round >= 13 THEN 1 END) * 100.0
                / NULLIF(COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 END), 0),
                0
            ),
            2
        ) AS long_term_maintenance_rate
    FROM contract_monthly_closing cmc
    GROUP BY cmc.closing_month, cmc.fp_id
) contract_summary
    ON contract_summary.closing_month = m.closing_month
   AND contract_summary.fp_id = u.id
LEFT JOIN (
    SELECT
        DATE_FORMAT(cs.consulted_at, '%Y-%m') AS closing_month,
        cs.fp_id,
        COUNT(*) AS consultation_count
    FROM consultations cs
    GROUP BY DATE_FORMAT(cs.consulted_at, '%Y-%m'), cs.fp_id
) consultation_summary
    ON consultation_summary.closing_month = m.closing_month
   AND consultation_summary.fp_id = u.id
LEFT JOIN (
    SELECT
        cs.closing_month,
        cs.fp_id,
        SUBSTRING_INDEX(
            GROUP_CONCAT(cs.consultation_channel ORDER BY cs.channel_count DESC, cs.consultation_channel SEPARATOR ','),
            ',',
            1
        ) AS consultation_channel
    FROM (
        SELECT
            DATE_FORMAT(consulted_at, '%Y-%m') AS closing_month,
            fp_id,
            consultation_channel,
            COUNT(*) AS channel_count
        FROM consultations
        GROUP BY DATE_FORMAT(consulted_at, '%Y-%m'), fp_id, consultation_channel
    ) cs
    GROUP BY cs.closing_month, cs.fp_id
) channel_summary
    ON channel_summary.closing_month = m.closing_month
   AND channel_summary.fp_id = u.id
LEFT JOIN (
    SELECT
        DATE_FORMAT(changed_at, '%Y-%m') AS closing_month,
        after_fp_id AS fp_id,
        COUNT(*) AS handover_success_count
    FROM customer_fp_history
    GROUP BY DATE_FORMAT(changed_at, '%Y-%m'), after_fp_id
) handover_summary
    ON handover_summary.closing_month = m.closing_month
   AND handover_summary.fp_id = u.id
WHERE u.user_role = 'FP'
  AND u.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_v16_recent_contract_targets;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_months_v16;
