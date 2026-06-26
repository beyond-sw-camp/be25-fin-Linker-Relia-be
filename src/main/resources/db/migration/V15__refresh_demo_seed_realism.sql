SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';
SET @REFERENCE_DATE = DATE('2026-06-24');

UPDATE organizations
SET organization_name = CASE organization_code
        WHEN 'HQ001' THEN '리리아 본사'
        WHEN 'BR001' THEN '강남지점'
        WHEN 'BR002' THEN '서초지점'
        WHEN 'BR003' THEN '송파지점'
        WHEN 'BR004' THEN '마포지점'
        WHEN 'BR005' THEN '영등포지점'
        WHEN 'BR006' THEN '용산지점'
        WHEN 'BR007' THEN '은평지점'
        WHEN 'BR008' THEN '노원지점'
        WHEN 'BR009' THEN '구로지점'
        WHEN 'BR010' THEN '강서지점'
        ELSE organization_name
    END,
    organization_address = CASE organization_code
        WHEN 'HQ001' THEN '서울특별시 중구 세종대로 110'
        WHEN 'BR001' THEN '서울특별시 강남구 테헤란로 152'
        WHEN 'BR002' THEN '서울특별시 서초구 강남대로 315'
        WHEN 'BR003' THEN '서울특별시 송파구 송파대로 201'
        WHEN 'BR004' THEN '서울특별시 마포구 월드컵북로 402'
        WHEN 'BR005' THEN '서울특별시 영등포구 국제금융로 24'
        WHEN 'BR006' THEN '서울특별시 용산구 한강대로 92'
        WHEN 'BR007' THEN '서울특별시 은평구 통일로 842'
        WHEN 'BR008' THEN '서울특별시 노원구 동일로 1382'
        WHEN 'BR009' THEN '서울특별시 구로구 디지털로 285'
        WHEN 'BR010' THEN '서울특별시 강서구 공항대로 247'
        ELSE organization_address
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE organization_code IN ('HQ001', 'BR001', 'BR002', 'BR003', 'BR004', 'BR005', 'BR006', 'BR007', 'BR008', 'BR009', 'BR010');

UPDATE users
SET user_name = CASE
        WHEN emp_code = 'SYS001' THEN '시스템관리자'
        WHEN user_role = 'HQ_MANAGER' THEN CONCAT(
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED), 12) + 1, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 3, 12) + 1, '민', '서', '지', '도', '하', '주', '유', '다', '현', '정', '재', '수'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 7, 12) + 1, '준', '연', '원', '윤', '아', '호', '림', '진', '현', '우', '영', '경')
        )
        WHEN user_role = 'BRANCH_MANAGER' THEN CONCAT(
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED), 12) + 1, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 5, 12) + 1, '서', '도', '하', '주', '유', '다', '현', '정', '재', '수', '민', '지'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 11, 12) + 1, '훈', '원', '린', '윤', '아', '호', '림', '진', '현', '우', '영', '경')
        )
        WHEN user_role = 'FP' THEN CONCAT(
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED), 14) + 1, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '신', '권'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 3, 14) + 1, '민', '서', '지', '도', '하', '주', '유', '다', '현', '정', '재', '수', '소', '나'),
            ELT(MOD(CAST(RIGHT(emp_code, 3) AS UNSIGNED) * 7, 14) + 1, '준', '연', '원', '윤', '아', '호', '림', '진', '현', '우', '영', '경', '빈', '솔')
        )
        ELSE user_name
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE user_role IN ('SYSTEM_ADMIN', 'HQ_MANAGER', 'BRANCH_MANAGER', 'FP');

UPDATE insurance_companies
SET insurance_company_name = CASE insurance_company_code
        WHEN 'LC001' THEN '삼성생명'
        WHEN 'LC002' THEN '한화생명'
        WHEN 'LC003' THEN '교보생명'
        WHEN 'LC004' THEN '신한라이프'
        WHEN 'LC005' THEN 'NH농협생명'
        ELSE insurance_company_name
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE insurance_company_code IN ('LC001', 'LC002', 'LC003', 'LC004', 'LC005');

UPDATE insurance_categories
SET insurance_category_name = CASE insurance_category_code
        WHEN 'CAT001' THEN '정기보험'
        WHEN 'CAT002' THEN '종신보험'
        WHEN 'CAT003' THEN '건강특약'
        WHEN 'CAT004' THEN '실손보험'
        WHEN 'CAT005' THEN 'CI/GI보험'
        WHEN 'CAT006' THEN '연금보험'
        ELSE insurance_category_name
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE insurance_category_code IN ('CAT001', 'CAT002', 'CAT003', 'CAT004', 'CAT005', 'CAT006');

UPDATE disease_codes
SET disease_name = CASE disease_code
        WHEN 'DIS001' THEN '고혈압'
        WHEN 'DIS002' THEN '당뇨병'
        WHEN 'DIS003' THEN '고지혈증'
        WHEN 'DIS004' THEN '협심증'
        WHEN 'DIS005' THEN '심근경색'
        WHEN 'DIS006' THEN '뇌졸중'
        WHEN 'DIS007' THEN '암'
        WHEN 'DIS008' THEN '갑상선암'
        WHEN 'DIS009' THEN '간질환'
        WHEN 'DIS010' THEN '신장질환'
        WHEN 'DIS011' THEN '우울증'
        WHEN 'DIS012' THEN '불안장애'
        WHEN 'DIS013' THEN '디스크질환'
        WHEN 'DIS014' THEN '천식'
        WHEN 'DIS015' THEN '갑상선질환'
        ELSE disease_name
    END,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE disease_code LIKE 'DIS%';

UPDATE insurance_products ip
JOIN insurance_companies ic ON ic.id = ip.insurance_company_id
JOIN insurance_categories cat ON cat.id = ip.insurance_category_id
SET ip.insurance_product_name = CONCAT(
        CASE MOD(CAST(RIGHT(ip.insurance_product_code, 3) AS UNSIGNED), 4)
            WHEN 0 THEN '스마트'
            WHEN 1 THEN '든든한'
            WHEN 2 THEN '밸런스'
            ELSE '케어플러스'
        END,
        ' ',
        cat.insurance_category_name,
        ' ',
        LPAD(CAST(RIGHT(ip.insurance_product_code, 3) AS UNSIGNED), 2, '0')
    ),
    ip.product_description = CONCAT(
        ic.insurance_company_name,
        '의 ',
        cat.insurance_category_name,
        ' 더미 상품 ',
        LPAD(CAST(RIGHT(ip.insurance_product_code, 3) AS UNSIGNED), 3, '0')
    ),
    ip.updated_at = CURRENT_TIMESTAMP,
    ip.updated_by = @SYSTEM_USER_ID
WHERE ip.insurance_product_code LIKE 'LP%';

UPDATE customers
SET customer_name = CONCAT(
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 14) + 1, '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '신', '권'),
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 3, 14) + 1, '민', '서', '지', '도', '하', '주', '유', '다', '현', '정', '재', '수', '소', '나'),
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 7, 14) + 1, '준', '연', '원', '윤', '아', '호', '림', '진', '현', '우', '영', '경', '빈', '솔')
    ),
    customer_email = CONCAT('customer', LPAD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 4, '0'), '@relia.co.kr'),
    customer_address_road = CONCAT(
        ELT(MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 10) + 1,
            '서울특별시 강남구 테헤란로',
            '서울특별시 서초구 강남대로',
            '서울특별시 송파구 송파대로',
            '서울특별시 마포구 월드컵북로',
            '서울특별시 영등포구 국제금융로',
            '서울특별시 용산구 한강대로',
            '서울특별시 은평구 통일로',
            '서울특별시 노원구 동일로',
            '서울특별시 구로구 디지털로',
            '서울특별시 강서구 공항대로'
        ),
        ' ',
        100 + MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 150)
    ),
    customer_address_detail = CONCAT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 18) + 2,
        '층 ',
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 24) + 101,
        '호'
    ),
    customer_job = ELT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED), 8) + 1,
        '회사원', '교사', '간호사', '개발자', '회계사', '자영업', '공무원', '디자이너'
    ),
    customer_company_name = ELT(
        MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 5, 8) + 1,
        '리리아파트너스', '서울교육지원청', '메디플러스병원', '넥스트코어', '알파파이낸스', '해솔유통', '한강모빌리티', '다온커머스'
    ),
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE customer_code LIKE 'CUS-%';

UPDATE contracts
SET coverage_summary = CONCAT('보장 설계안 ', LPAD(CAST(RIGHT(contract_code, 6) AS UNSIGNED), 6, '0')),
    updated_at = CURRENT_TIMESTAMP,
    updated_by = @SYSTEM_USER_ID
WHERE contract_code LIKE 'CTR%';

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_ADD(
            DATE_ADD('2025-12-05', INTERVAL ((target.seq_no - 1) DIV 5) MONTH),
            INTERVAL (MOD(target.seq_no - 1, 5) * 4) DAY
        ) AS recent_contract_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY selected_target.contract_code) AS seq_no,
            selected_target.contract_id
        FROM (
            SELECT
                ct.id AS contract_id,
                ct.contract_code
            FROM contracts ct
            WHERE ct.contract_status = 'MAINTENANCE'
              AND ct.contract_code LIKE 'CTR%'
            ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 61, 257),
                     MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 127),
                     ct.contract_code
            LIMIT 30
        ) selected_target
    ) target
) recent_contract_targets ON recent_contract_targets.contract_id = ct.id
SET ct.contract_date = recent_contract_targets.recent_contract_date,
    ct.contract_start_date = recent_contract_targets.recent_contract_date,
    ct.coverage_start_date = recent_contract_targets.recent_contract_date,
    ct.contract_end_date = DATE_ADD(recent_contract_targets.recent_contract_date, INTERVAL ct.payment_period_years YEAR),
    ct.coverage_end_date = DATE_ADD(recent_contract_targets.recent_contract_date, INTERVAL ct.payment_period_years YEAR),
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

DROP TEMPORARY TABLE IF EXISTS tmp_demo_months;
CREATE TEMPORARY TABLE tmp_demo_months (
    month_seq INT NOT NULL,
    closing_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    closing_date DATE NOT NULL,
    PRIMARY KEY (month_seq)
);

INSERT INTO tmp_demo_months (month_seq, closing_month, closing_date)
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
    CONCAT(
        '61500000-0000-0000-0000-',
        LPAD(ROW_NUMBER() OVER (ORDER BY closing_seed.closing_month, closing_seed.contract_code), 12, '0')
    ) AS id,
    closing_seed.closing_month,
    closing_seed.contract_id,
    closing_seed.snapshot_contract_status,
    closing_seed.payment_status,
    closing_seed.scheduled_payment_round AS current_payment_round,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'MAINTENANCE' THEN closing_seed.scheduled_payment_round
        ELSE NULL
    END AS maintenance_round,
    CASE WHEN closing_seed.snapshot_contract_status = 'LAPSED' THEN TRUE ELSE FALSE END AS lapse_yn,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'LAPSED' THEN closing_seed.lapse_at
        ELSE NULL
    END AS lapse_at,
    CASE WHEN closing_seed.snapshot_contract_status = 'TERMINATED' THEN TRUE ELSE FALSE END AS terminated_yn,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'TERMINATED' THEN closing_seed.terminated_at
        ELSE NULL
    END AS terminated_at,
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
                     OR (
                         base_seed.snapshot_contract_status = 'LAPSED'
                         AND base_seed.month_seq = 6
                     )
                 )
                THEN 'UNPAID'
            ELSE 'PAID'
        END AS payment_status
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
        JOIN tmp_demo_months months
            ON ct.contract_start_date <= months.closing_date
        LEFT JOIN (
            SELECT
                contract_id,
                CASE MOD(seq_no, 10)
                    WHEN 0 THEN 1
                    WHEN 1 THEN 1
                    WHEN 2 THEN 1
                    WHEN 3 THEN 2
                    WHEN 4 THEN 2
                    WHEN 5 THEN 2
                    WHEN 6 THEN 2
                    WHEN 7 THEN 3
                    WHEN 8 THEN 3
                    ELSE 3
                END AS unpaid_installment_count
            FROM (
                SELECT
                    ROW_NUMBER() OVER (ORDER BY ct.contract_code DESC) AS seq_no,
                    ct.id AS contract_id
                FROM contracts ct
                WHERE ct.contract_status IN ('MAINTENANCE', 'LAPSED')
                  AND ct.contract_code LIKE 'CTR%'
                  AND ct.contract_date < '2026-01-01'
                ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 59, 251) DESC,
                         MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 113) DESC,
                         ct.contract_code DESC
                LIMIT 32
            ) unpaid_seed
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
    CONCAT('82500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY c.customer_code), 12, '0')) AS id,
    1 AS customer_status_sequence,
    c.id,
    NULL AS before_status,
    'PROSPECT' AS after_status,
    '신규 고객 생성' AS changed_reason,
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
    CONCAT('82600000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY contract_status_seed.customer_code), 12, '0')) AS id,
    2 AS customer_status_sequence,
    contract_status_seed.customer_id,
    'PROSPECT' AS before_status,
    'CONTRACTED' AS after_status,
    '계약 등록' AS changed_reason,
    contract_status_seed.first_contract_date AS changed_at,
    @SYSTEM_USER_ID
FROM (
    SELECT
        c.customer_code,
        c.id AS customer_id,
        MIN(ct.contract_date) AS first_contract_date
    FROM customers c
    JOIN contracts ct ON ct.customer_id = c.id
    GROUP BY c.customer_code, c.id
) contract_status_seed;

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
    CONCAT('82700000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY closed_seed.customer_code), 12, '0')) AS id,
    3 AS customer_status_sequence,
    closed_seed.customer_id,
    'CONTRACTED' AS before_status,
    'CLOSED' AS after_status,
    '계약 종료 반영' AS changed_reason,
    closed_seed.closed_at AS changed_at,
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
JOIN (
    SELECT
        ranked.id,
        ranked.consultation_type,
        ranked.seq_no,
        CASE ranked.consultation_type
            WHEN 'NEW_CONTRACT' THEN DATE_ADD('2026-05-01 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 24) DAY)
            WHEN 'RENEWAL' THEN DATE_ADD('2026-05-03 14:00:00', INTERVAL MOD(ranked.seq_no - 1, 26) DAY)
            WHEN 'CLAIM' THEN DATE_ADD('2026-04-05 11:00:00', INTERVAL MOD(ranked.seq_no - 1, 28) DAY)
            ELSE DATE_ADD('2026-04-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 20) DAY)
        END AS new_consulted_at
    FROM (
        SELECT
            c0.id,
            c0.consultation_type,
            ROW_NUMBER() OVER (PARTITION BY c0.consultation_type ORDER BY c0.id) AS seq_no
        FROM consultations c0
    ) ranked
) schedule_seed ON schedule_seed.id = cs.id
JOIN customers c ON c.id = cs.customer_id
SET cs.consulted_at = schedule_seed.new_consulted_at,
    cs.next_scheduled_at = CASE
        WHEN c.customer_status = 'CLOSED' THEN NULL
        WHEN schedule_seed.consultation_type = 'TERMINATION' THEN NULL
        WHEN MOD(schedule_seed.seq_no, 4) = 0 THEN NULL
        ELSE DATE_ADD(schedule_seed.new_consulted_at, INTERVAL 7 + MOD(schedule_seed.seq_no, 21) DAY)
    END,
    cs.updated_at = CURRENT_TIMESTAMP,
    cs.updated_by = @SYSTEM_USER_ID;

UPDATE consultation_renewal_details rd
JOIN consultations cs ON cs.id = rd.consultation_id
SET rd.renewal_scheduled_date = DATE_ADD(DATE(cs.consulted_at), INTERVAL 14 + MOD(CAST(RIGHT(rd.id, 3) AS UNSIGNED), 14) DAY),
    rd.updated_at = CURRENT_TIMESTAMP,
    rd.updated_by = @SYSTEM_USER_ID;

UPDATE consultation_claim_details cd
JOIN consultations cs ON cs.id = cd.consultation_id
SET cd.incident_date = DATE_SUB(DATE(cs.consulted_at), INTERVAL 3 + MOD(CAST(RIGHT(cd.id, 3) AS UNSIGNED), 18) DAY),
    cd.updated_at = CURRENT_TIMESTAMP,
    cd.updated_by = @SYSTEM_USER_ID;

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
    CONCAT('90500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (
        ORDER BY commission_source.sort_order, commission_source.contract_code, commission_source.commission_month
    ), 12, '0')) AS id,
    commission_source.commission_month,
    commission_source.contract_id,
    commission_source.insurance_company_id,
    commission_source.insurance_product_id,
    commission_source.commission_type,
    commission_source.gross_commission_amount,
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
        recovery_source.contract_code,
        recovery_source.contract_id,
        recovery_source.insurance_company_id,
        recovery_source.insurance_product_id,
        recovery_source.commission_month,
        'RECOVERY' AS commission_type,
        recovery_source.gross_commission_amount
    FROM (
        SELECT
            ct.contract_code,
            ct.id AS contract_id,
            ip.insurance_company_id,
            ct.insurance_product_id,
            cmc.closing_month AS commission_month,
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
        WHERE ((cmc.contract_status = 'LAPSED'
            AND cmc.lapse_yn = TRUE
            AND DATE_FORMAT(cmc.lapse_at, '%Y-%m') = cmc.closing_month)
            OR (cmc.contract_status = 'TERMINATED'
            AND cmc.terminated_yn = TRUE
            AND DATE_FORMAT(cmc.terminated_at, '%Y-%m') = cmc.closing_month))
          AND ct.contract_code LIKE 'CTR%'
    ) recovery_source
    WHERE recovery_source.gross_commission_amount > 0
) commission_source;

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
    CONCAT('91500000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    commission_month,
    gross_commission_id,
    contract_id,
    fp_id,
    organization_id,
    insurance_company_id,
    insurance_product_id,
    CASE gross_type
        WHEN 'INITIAL' THEN 'INITIAL_PAYMENT'
        WHEN 'MAINTENANCE' THEN 'MAINTENANCE_PAYMENT'
        ELSE 'RECOVERY_COLLECTION'
    END AS commission_type,
    gross_commission_amount AS base_commission_amount,
    fp_payment_rate,
    ROUND(gross_commission_amount * (fp_payment_rate / 100), 2) AS commission_amount,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY gcr.id) AS seq_no,
        gcr.id AS gross_commission_id,
        gcr.commission_month,
        gcr.contract_id,
        ct.fp_id,
        fp.organization_id,
        gcr.insurance_company_id,
        gcr.insurance_product_id,
        gcr.commission_type AS gross_type,
        gcr.gross_commission_amount,
        CAST(70.00 AS DECIMAL(5,2)) AS fp_payment_rate
    FROM gross_commission_records gcr
    JOIN contracts ct ON ct.id = gcr.contract_id
    JOIN users fp ON fp.id = ct.fp_id
) payment_seed;

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
    CONCAT('92500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY fp_source.closing_month, fp_source.fp_id), 12, '0')) AS id,
    fp_source.closing_month,
    fp_source.fp_id,
    fp_source.organization_id,
    fp_source.total_initial_payment_amount,
    fp_source.total_maintenance_payment_amount,
    fp_source.total_recovery_collection_amount,
    fp_source.total_payment_amount,
    fp_source.net_commission_amount,
    fp_source.contract_count,
    fp_source.recovery_contract_count,
    fp_source.closed_at
FROM (
    SELECT
        pcr.commission_month AS closing_month,
        pcr.fp_id,
        pcr.organization_id,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'INITIAL_PAYMENT' THEN pcr.commission_amount ELSE 0 END), 2) AS total_initial_payment_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'MAINTENANCE_PAYMENT' THEN pcr.commission_amount ELSE 0 END), 2) AS total_maintenance_payment_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_recovery_collection_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_amount,
        ROUND(
            SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END)
            - SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END),
            2
        ) AS net_commission_amount,
        COUNT(DISTINCT pcr.contract_id) AS contract_count,
        COUNT(DISTINCT CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.contract_id END) AS recovery_contract_count,
        COALESCE(
            MAX(cmc.closed_at),
            TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(pcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
        ) AS closed_at
    FROM payment_commission_records pcr
    LEFT JOIN contract_monthly_closing cmc
        ON cmc.contract_id = pcr.contract_id
       AND cmc.closing_month = pcr.commission_month
    GROUP BY
        pcr.commission_month,
        pcr.fp_id,
        pcr.organization_id
) fp_source;

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
    CONCAT('92600000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY branch_source.closing_month, branch_source.organization_id), 12, '0')) AS id,
    branch_source.closing_month,
    branch_source.organization_id,
    branch_source.total_initial_payment_amount,
    branch_source.total_maintenance_payment_amount,
    branch_source.total_recovery_collection_amount,
    branch_source.total_payment_amount,
    branch_source.net_commission_amount,
    branch_source.fp_count,
    branch_source.contract_count,
    branch_source.recovery_contract_count,
    branch_source.closed_at
FROM (
    SELECT
        fp_closing.closing_month,
        fp_closing.organization_id,
        ROUND(SUM(fp_closing.total_initial_payment_amount), 2) AS total_initial_payment_amount,
        ROUND(SUM(fp_closing.total_maintenance_payment_amount), 2) AS total_maintenance_payment_amount,
        ROUND(SUM(fp_closing.total_recovery_collection_amount), 2) AS total_recovery_collection_amount,
        ROUND(SUM(fp_closing.total_payment_amount), 2) AS total_payment_amount,
        ROUND(SUM(fp_closing.net_commission_amount), 2) AS net_commission_amount,
        COUNT(DISTINCT fp_closing.fp_id) AS fp_count,
        SUM(fp_closing.contract_count) AS contract_count,
        SUM(fp_closing.recovery_contract_count) AS recovery_contract_count,
        MAX(fp_closing.closed_at) AS closed_at
    FROM fp_commission_monthly_closing fp_closing
    GROUP BY fp_closing.closing_month, fp_closing.organization_id
) branch_source;

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
    CONCAT('92700000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY income_source.closing_month), 12, '0')) AS id,
    income_source.closing_month,
    income_source.net_income_commission_amount,
    income_source.total_initial_gross_commission_amount,
    income_source.total_maintenance_gross_commission_amount,
    income_source.total_payment_commission_amount,
    income_source.total_insurance_recovery_amount,
    income_source.total_fp_recovery_collection_amount,
    income_source.closed_at
FROM (
    SELECT
        gross_summary.closing_month,
        ROUND(
            gross_summary.total_initial_gross_commission_amount
            + gross_summary.total_maintenance_gross_commission_amount
            - payment_summary.total_payment_commission_amount
            - gross_summary.total_insurance_recovery_amount
            + payment_summary.total_fp_recovery_collection_amount,
            2
        ) AS net_income_commission_amount,
        gross_summary.total_initial_gross_commission_amount,
        gross_summary.total_maintenance_gross_commission_amount,
        payment_summary.total_payment_commission_amount,
        gross_summary.total_insurance_recovery_amount,
        payment_summary.total_fp_recovery_collection_amount,
        COALESCE(gross_summary.closed_at, payment_summary.closed_at) AS closed_at
    FROM (
        SELECT
            gcr.commission_month AS closing_month,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
            COALESCE(
                MAX(cmc.closed_at),
                TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(gcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
            ) AS closed_at
        FROM gross_commission_records gcr
        LEFT JOIN contract_monthly_closing cmc
            ON cmc.contract_id = gcr.contract_id
           AND cmc.closing_month = gcr.commission_month
        GROUP BY gcr.commission_month
    ) gross_summary
    JOIN (
        SELECT
            pcr.commission_month AS closing_month,
            ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_commission_amount,
            ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_fp_recovery_collection_amount,
            COALESCE(
                MAX(cmc.closed_at),
                TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(pcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
            ) AS closed_at
        FROM payment_commission_records pcr
        LEFT JOIN contract_monthly_closing cmc
            ON cmc.contract_id = pcr.contract_id
           AND cmc.closing_month = pcr.commission_month
        GROUP BY pcr.commission_month
    ) payment_summary
        ON payment_summary.closing_month = gross_summary.closing_month
) income_source;

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
    CONCAT('92800000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY branch_income_source.closing_month, branch_income_source.organization_id), 12, '0')) AS id,
    branch_income_source.closing_month,
    branch_income_source.organization_id,
    branch_income_source.net_income_commission_amount,
    branch_income_source.total_initial_gross_commission_amount,
    branch_income_source.total_maintenance_gross_commission_amount,
    branch_income_source.total_gross_commission_amount,
    branch_income_source.total_payment_commission_amount,
    branch_income_source.total_insurance_recovery_amount,
    branch_income_source.total_fp_recovery_collection_amount,
    branch_income_source.contract_count,
    branch_income_source.fp_count,
    branch_income_source.closed_at
FROM (
    SELECT
        gross_summary.closing_month,
        gross_summary.organization_id,
        ROUND(
            gross_summary.total_initial_gross_commission_amount
            + gross_summary.total_maintenance_gross_commission_amount
            - payment_summary.total_payment_commission_amount
            - gross_summary.total_insurance_recovery_amount
            + payment_summary.total_fp_recovery_collection_amount,
            2
        ) AS net_income_commission_amount,
        gross_summary.total_initial_gross_commission_amount,
        gross_summary.total_maintenance_gross_commission_amount,
        gross_summary.total_gross_commission_amount,
        payment_summary.total_payment_commission_amount,
        gross_summary.total_insurance_recovery_amount,
        payment_summary.total_fp_recovery_collection_amount,
        gross_summary.contract_count,
        gross_summary.fp_count,
        COALESCE(gross_summary.closed_at, payment_summary.closed_at) AS closed_at
    FROM (
        SELECT
            gcr.commission_month AS closing_month,
            fp.organization_id,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
            ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
            ROUND(SUM(CASE WHEN gcr.commission_type IN ('INITIAL', 'MAINTENANCE') THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_gross_commission_amount,
            COUNT(DISTINCT gcr.contract_id) AS contract_count,
            COUNT(DISTINCT ct.fp_id) AS fp_count,
            COALESCE(
                MAX(cmc.closed_at),
                TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(gcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
            ) AS closed_at
        FROM gross_commission_records gcr
        JOIN contracts ct ON ct.id = gcr.contract_id
        JOIN users fp ON fp.id = ct.fp_id
        LEFT JOIN contract_monthly_closing cmc
            ON cmc.contract_id = gcr.contract_id
           AND cmc.closing_month = gcr.commission_month
        GROUP BY gcr.commission_month, fp.organization_id
    ) gross_summary
    JOIN (
        SELECT
            pcr.commission_month AS closing_month,
            pcr.organization_id,
            ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_commission_amount,
            ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_fp_recovery_collection_amount,
            COALESCE(
                MAX(cmc.closed_at),
                TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(pcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00')
            ) AS closed_at
        FROM payment_commission_records pcr
        LEFT JOIN contract_monthly_closing cmc
            ON cmc.contract_id = pcr.contract_id
           AND cmc.closing_month = pcr.commission_month
        GROUP BY pcr.commission_month, pcr.organization_id
    ) payment_summary
        ON payment_summary.closing_month = gross_summary.closing_month
       AND payment_summary.organization_id = gross_summary.organization_id
) branch_income_source;

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
    CONCAT('93500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY m.closing_month, u.emp_code), 12, '0')) AS id,
    m.closing_month,
    u.emp_code,
    u.user_name,
    o.organization_code,
    o.organization_name,
    o.organization_type,
    GREATEST(1, TIMESTAMPDIFF(YEAR, u.joined_at, m.closing_date)) AS career_years,
    ELT(MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 6) + 1, '건강보험', '종신보험', '연금보험', '실손보험', '가족보장', '기업보장') AS specialty_category,
    30 + MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 18) AS preferred_customer_age,
    ELT(MOD(CAST(RIGHT(u.emp_code, 3) AS UNSIGNED), 3) + 1, 'LOW', 'MIDDLE', 'HIGH') AS preferred_customer_asset_level,
    COALESCE(channel_summary.consultation_channel, 'PHONE') AS consultation_channel,
    COALESCE(contract_summary.current_contract_count, 0) AS current_contract_count,
    COALESCE(contract_summary.retention_rate, 0.00) AS retention_rate,
    COALESCE(consultation_summary.consultation_count, 0) AS consultation_count,
    COALESCE(handover_summary.handover_success_count, 0) AS handover_success_count,
    COALESCE(contract_summary.long_term_maintenance_rate, 0.00) AS long_term_maintenance_rate,
    TIMESTAMP(m.closing_date, '18:00:00') AS created_at,
    @SYSTEM_USER_ID
FROM users u
JOIN organizations o ON o.id = u.organization_id
JOIN tmp_demo_months m
LEFT JOIN (
    SELECT
        cmc.closing_month,
        cmc.fp_id,
        COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 END) AS current_contract_count,
        ROUND(
            COALESCE(
                COUNT(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 END) * 100.0
                / NULLIF(COUNT(*), 0),
                0
            ),
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
            GROUP_CONCAT(cs.consultation_channel ORDER BY channel_count DESC, cs.consultation_channel SEPARATOR ','),
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

DROP TEMPORARY TABLE IF EXISTS tmp_demo_months;
