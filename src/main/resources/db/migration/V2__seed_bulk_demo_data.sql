-- ============================================================================
-- V2 bulk seed baseline
-- - Relia life insurance demo data
-- - master/reference data + users baseline
-- - customer/contract/consultation bulk sections will be appended in this file
-- ============================================================================

SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';
SET @PASSWORD = '$2a$10$hBUPY5Nl8.bzB6kYvf7n4e9WlQiT410uWhgEOTT1CyubCxvS5hUZu';

SET @HQ_ID = '10000000-0000-0000-0000-000000000001';
SET @BR001 = '20000000-0000-0000-0000-000000000001';
SET @BR002 = '20000000-0000-0000-0000-000000000002';
SET @BR003 = '20000000-0000-0000-0000-000000000003';
SET @BR004 = '20000000-0000-0000-0000-000000000004';
SET @BR005 = '20000000-0000-0000-0000-000000000005';
SET @BR006 = '20000000-0000-0000-0000-000000000006';
SET @BR007 = '20000000-0000-0000-0000-000000000007';
SET @BR008 = '20000000-0000-0000-0000-000000000008';
SET @BR009 = '20000000-0000-0000-0000-000000000009';
SET @BR010 = '20000000-0000-0000-0000-000000000010';

INSERT INTO organizations (
    id,
    organization_code,
    parent_organization_id,
    organization_name,
    organization_type,
    organization_address,
    organization_phone,
    organization_status,
    created_by,
    updated_by
)
VALUES
    (@HQ_ID, 'HQ001', NULL, 'Relia 본사', 'HQ', '서울특별시 중구 세종대로 110', '02-1111-1111', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR001, 'BR001', @HQ_ID, '강남지점', 'BRANCH', '서울특별시 강남구 테헤란로 152', '02-2222-0001', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR002, 'BR002', @HQ_ID, '서초지점', 'BRANCH', '서울특별시 서초구 강남대로 315', '02-2222-0002', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR003, 'BR003', @HQ_ID, '송파지점', 'BRANCH', '서울특별시 송파구 송파대로 201', '02-2222-0003', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR004, 'BR004', @HQ_ID, '마포지점', 'BRANCH', '서울특별시 마포구 월드컵북로 402', '02-2222-0004', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR005, 'BR005', @HQ_ID, '영등포지점', 'BRANCH', '서울특별시 영등포구 국제금융로 24', '02-2222-0005', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR006, 'BR006', @HQ_ID, '용산지점', 'BRANCH', '서울특별시 용산구 한강대로 92', '02-2222-0006', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR007, 'BR007', @HQ_ID, '은평지점', 'BRANCH', '서울특별시 은평구 통일로 842', '02-2222-0007', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR008, 'BR008', @HQ_ID, '노원지점', 'BRANCH', '서울특별시 노원구 동일로 1382', '02-2222-0008', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR009, 'BR009', @HQ_ID, '구로지점', 'BRANCH', '서울특별시 구로구 디지털로 285', '02-2222-0009', 'ACTIVE', @HQ_ID, @HQ_ID),
    (@BR010, 'BR010', @HQ_ID, '강서지점', 'BRANCH', '서울특별시 강서구 공항대로 247', '02-2222-0010', 'ACTIVE', @HQ_ID, @HQ_ID);

INSERT INTO users (
    id,
    emp_code,
    login_id,
    password,
    user_name,
    user_role,
    organization_id,
    user_status,
    phone,
    email,
    joined_at,
    resigned_at,
    last_login_at,
    created_by,
    updated_by
)
VALUES
    (
        @SYSTEM_USER_ID,
        'SYS001',
        'sysadmin',
        @PASSWORD,
        '시스템관리자',
        'SYSTEM_ADMIN',
        @HQ_ID,
        'ACTIVE',
        '010-0000-0001',
        'sysadmin@relia.com',
        '2024-01-01',
        NULL,
        NULL,
        @SYSTEM_USER_ID,
        @SYSTEM_USER_ID
    );

INSERT INTO users (
    id,
    emp_code,
    login_id,
    password,
    user_name,
    user_role,
    organization_id,
    user_status,
    phone,
    email,
    joined_at,
    resigned_at,
    last_login_at,
    created_by,
    updated_by
)
SELECT
    CONCAT('30000000-0000-0000-0000-', LPAD(n + 1, 12, '0')) AS id,
    CONCAT('HQ', LPAD(n, 3, '0')) AS emp_code,
    CONCAT('hqmanager', LPAD(n, 2, '0')) AS login_id,
    @PASSWORD,
    CONCAT(
        CASE MOD(n, 10)
            WHEN 0 THEN '김'
            WHEN 1 THEN '이'
            WHEN 2 THEN '박'
            WHEN 3 THEN '최'
            WHEN 4 THEN '정'
            WHEN 5 THEN '강'
            WHEN 6 THEN '조'
            WHEN 7 THEN '윤'
            WHEN 8 THEN '장'
            ELSE '임'
        END,
        CASE MOD(n + 2, 10)
            WHEN 0 THEN '민'
            WHEN 1 THEN '서'
            WHEN 2 THEN '지'
            WHEN 3 THEN '도'
            WHEN 4 THEN '하'
            WHEN 5 THEN '주'
            WHEN 6 THEN '현'
            WHEN 7 THEN '시'
            WHEN 8 THEN '유'
            ELSE '은'
        END,
        CASE MOD(n + 5, 10)
            WHEN 0 THEN '준'
            WHEN 1 THEN '연'
            WHEN 2 THEN '우'
            WHEN 3 THEN '민'
            WHEN 4 THEN '아'
            WHEN 5 THEN '윤'
            WHEN 6 THEN '호'
            WHEN 7 THEN '현'
            WHEN 8 THEN '진'
            ELSE '원'
        END
    ) AS user_name,
    'HQ_MANAGER',
    @HQ_ID,
    'ACTIVE',
    CONCAT('010-1000-', LPAD(n, 4, '0')) AS phone,
    CONCAT('hqmanager', LPAD(n, 2, '0'), '@relia.com') AS email,
    DATE_ADD('2024-01-01', INTERVAL n DAY) AS joined_at,
    NULL,
    NULL,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) numbers;

INSERT INTO users (
    id,
    emp_code,
    login_id,
    password,
    user_name,
    user_role,
    organization_id,
    user_status,
    phone,
    email,
    joined_at,
    resigned_at,
    last_login_at,
    created_by,
    updated_by
)
SELECT
    CONCAT('30000000-0000-0000-0000-', LPAD(n + 11, 12, '0')) AS id,
    CONCAT('BM', LPAD(n, 3, '0')) AS emp_code,
    CONCAT('branchmanager', LPAD(n, 2, '0')) AS login_id,
    @PASSWORD,
    CONCAT(
        CASE MOD(n + 1, 10)
            WHEN 0 THEN '김'
            WHEN 1 THEN '이'
            WHEN 2 THEN '박'
            WHEN 3 THEN '최'
            WHEN 4 THEN '정'
            WHEN 5 THEN '강'
            WHEN 6 THEN '조'
            WHEN 7 THEN '윤'
            WHEN 8 THEN '장'
            ELSE '임'
        END,
        CASE MOD(n + 3, 10)
            WHEN 0 THEN '민'
            WHEN 1 THEN '서'
            WHEN 2 THEN '지'
            WHEN 3 THEN '도'
            WHEN 4 THEN '하'
            WHEN 5 THEN '주'
            WHEN 6 THEN '현'
            WHEN 7 THEN '시'
            WHEN 8 THEN '유'
            ELSE '은'
        END,
        CASE MOD(n + 6, 10)
            WHEN 0 THEN '준'
            WHEN 1 THEN '연'
            WHEN 2 THEN '우'
            WHEN 3 THEN '민'
            WHEN 4 THEN '아'
            WHEN 5 THEN '윤'
            WHEN 6 THEN '호'
            WHEN 7 THEN '현'
            WHEN 8 THEN '진'
            ELSE '원'
        END
    ) AS user_name,
    'BRANCH_MANAGER',
    CONCAT('20000000-0000-0000-0000-', LPAD(n, 12, '0')) AS organization_id,
    'ACTIVE',
    CONCAT('010-2000-', LPAD(n, 4, '0')) AS phone,
    CONCAT('branchmanager', LPAD(n, 2, '0'), '@relia.com') AS email,
    DATE_ADD('2024-02-01', INTERVAL n DAY) AS joined_at,
    NULL,
    NULL,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) numbers;

INSERT INTO users (
    id,
    emp_code,
    login_id,
    password,
    user_name,
    user_role,
    organization_id,
    user_status,
    phone,
    email,
    joined_at,
    resigned_at,
    last_login_at,
    created_by,
    updated_by
)
SELECT
    CONCAT('30000000-0000-0000-0000-', LPAD(n + 21, 12, '0')) AS id,
    CONCAT('FP', LPAD(n, 3, '0')) AS emp_code,
    CONCAT('fp', LPAD(n, 3, '0')) AS login_id,
    @PASSWORD,
    CONCAT(
        CASE MOD(n + 2, 12)
            WHEN 0 THEN '김'
            WHEN 1 THEN '이'
            WHEN 2 THEN '박'
            WHEN 3 THEN '최'
            WHEN 4 THEN '정'
            WHEN 5 THEN '강'
            WHEN 6 THEN '조'
            WHEN 7 THEN '윤'
            WHEN 8 THEN '장'
            WHEN 9 THEN '임'
            WHEN 10 THEN '한'
            ELSE '오'
        END,
        CASE MOD(n + 4, 15)
            WHEN 0 THEN '민'
            WHEN 1 THEN '서'
            WHEN 2 THEN '지'
            WHEN 3 THEN '도'
            WHEN 4 THEN '하'
            WHEN 5 THEN '주'
            WHEN 6 THEN '현'
            WHEN 7 THEN '시'
            WHEN 8 THEN '유'
            WHEN 9 THEN '은'
            WHEN 10 THEN '태'
            WHEN 11 THEN '수'
            WHEN 12 THEN '재'
            WHEN 13 THEN '가'
            ELSE '다'
        END,
        CASE MOD(n + 9, 15)
            WHEN 0 THEN '준'
            WHEN 1 THEN '연'
            WHEN 2 THEN '우'
            WHEN 3 THEN '민'
            WHEN 4 THEN '아'
            WHEN 5 THEN '윤'
            WHEN 6 THEN '호'
            WHEN 7 THEN '현'
            WHEN 8 THEN '진'
            WHEN 9 THEN '원'
            WHEN 10 THEN '림'
            WHEN 11 THEN '혁'
            WHEN 12 THEN '서'
            WHEN 13 THEN '빈'
            ELSE '율'
        END
    ) AS user_name,
    'FP',
    CONCAT('20000000-0000-0000-0000-', LPAD(((n - 1) DIV 10) + 1, 12, '0')) AS organization_id,
    'ACTIVE',
    CONCAT('010-3000-', LPAD(n, 4, '0')) AS phone,
    CONCAT('fp', LPAD(n, 3, '0'), '@relia.com') AS email,
    DATE_ADD('2024-03-01', INTERVAL n DAY) AS joined_at,
    NULL,
    NULL,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT ones.n + (tens.n * 10) + 1 AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
    WHERE ones.n + (tens.n * 10) < 100
) numbers;

INSERT INTO insurance_companies (
    id,
    insurance_company_code,
    insurance_company_name,
    insurance_company_status,
    insurance_company_phone,
    created_by,
    updated_by
)
VALUES
    ('50000000-0000-0000-0000-000000000001', 'LC001', '삼성생명', 'ACTIVE', '1588-1001', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('50000000-0000-0000-0000-000000000002', 'LC002', '한화생명', 'ACTIVE', '1588-1002', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('50000000-0000-0000-0000-000000000003', 'LC003', '교보생명', 'ACTIVE', '1588-1003', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('50000000-0000-0000-0000-000000000004', 'LC004', '신한라이프', 'ACTIVE', '1588-1004', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('50000000-0000-0000-0000-000000000005', 'LC005', 'NH농협생명', 'ACTIVE', '1588-1005', @SYSTEM_USER_ID, @SYSTEM_USER_ID);

INSERT INTO insurance_categories (
    id,
    insurance_category_code,
    insurance_category_name,
    insurance_category_status,
    created_by,
    updated_by
)
VALUES
    ('51000000-0000-0000-0000-000000000001', 'CAT001', '정기보험', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('51000000-0000-0000-0000-000000000002', 'CAT002', '종신보험', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('51000000-0000-0000-0000-000000000003', 'CAT003', '건강특약형', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('51000000-0000-0000-0000-000000000004', 'CAT004', '암보험', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('51000000-0000-0000-0000-000000000005', 'CAT005', 'CI_GI보험', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('51000000-0000-0000-0000-000000000006', 'CAT006', '연금보험', 'ACTIVE', @SYSTEM_USER_ID, @SYSTEM_USER_ID);

INSERT INTO insurance_products (
    id,
    insurance_product_code,
    insurance_company_id,
    insurance_category_id,
    insurance_product_name,
    insurance_product_status,
    insurance_start_date,
    insurance_end_date,
    coverage_period_type,
    coverage_period_years,
    coverage_age_limit,
    is_lifetime_coverage,
    is_renewable,
    renewal_cycle,
    product_description,
    created_by,
    updated_by
)
SELECT
    CONCAT('52000000-0000-0000-0000-', LPAD(n, 12, '0')) AS id,
    CONCAT('LP', LPAD(n, 3, '0')) AS insurance_product_code,
    CONCAT('50000000-0000-0000-0000-', LPAD(((n - 1) MOD 5) + 1, 12, '0')) AS insurance_company_id,
    CONCAT('51000000-0000-0000-0000-', LPAD(((n - 1) MOD 6) + 1, 12, '0')) AS insurance_category_id,
    CASE ((n - 1) MOD 6) + 1
        WHEN 1 THEN CONCAT('스마트정기보험 ', LPAD(n, 2, '0'))
        WHEN 2 THEN CONCAT('든든종신보험 ', LPAD(n, 2, '0'))
        WHEN 3 THEN CONCAT('건강특약플랜 ', LPAD(n, 2, '0'))
        WHEN 4 THEN CONCAT('암보장플랜 ', LPAD(n, 2, '0'))
        WHEN 5 THEN CONCAT('CI GI 케어보험 ', LPAD(n, 2, '0'))
        ELSE CONCAT('평생연금보험 ', LPAD(n, 2, '0'))
    END AS insurance_product_name,
    'ACTIVE',
    '2024-01-01',
    NULL,
    CASE ((n - 1) MOD 6) + 1
        WHEN 1 THEN 'YEARS'
        WHEN 2 THEN 'LIFETIME'
        WHEN 3 THEN 'YEARS'
        WHEN 4 THEN 'YEARS'
        WHEN 5 THEN 'AGE'
        ELSE 'YEARS'
    END AS coverage_period_type,
    CASE ((n - 1) MOD 6) + 1
        WHEN 1 THEN 20
        WHEN 2 THEN NULL
        WHEN 3 THEN 20
        WHEN 4 THEN 20
        WHEN 5 THEN NULL
        ELSE 15
    END AS coverage_period_years,
    CASE ((n - 1) MOD 6) + 1
        WHEN 5 THEN 90
        ELSE NULL
    END AS coverage_age_limit,
    CASE ((n - 1) MOD 6) + 1
        WHEN 2 THEN TRUE
        ELSE FALSE
    END AS is_lifetime_coverage,
    CASE ((n - 1) MOD 6) + 1
        WHEN 2 THEN FALSE
        WHEN 5 THEN FALSE
        ELSE TRUE
    END AS is_renewable,
    CASE ((n - 1) MOD 6) + 1
        WHEN 1 THEN 10
        WHEN 2 THEN NULL
        WHEN 3 THEN 5
        WHEN 4 THEN 5
        WHEN 5 THEN NULL
        ELSE 10
    END AS renewal_cycle,
    CONCAT('생명보험 시연용 상품 더미 데이터 ', LPAD(n, 3, '0')) AS product_description,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
    UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
    UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18
    UNION ALL SELECT 19 UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
) numbers;

INSERT INTO disease_codes (
    id,
    disease_code,
    disease_name,
    disease_category,
    insurance_notice_required_yn,
    created_by,
    updated_by
)
VALUES
    ('53000000-0000-0000-0000-000000000001', 'DIS001', 'HYPERTENSION', 'CARDIO', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000002', 'DIS002', 'DIABETES', 'METABOLIC', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000003', 'DIS003', 'HYPERLIPIDEMIA', 'METABOLIC', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000004', 'DIS004', 'ANGINA', 'CARDIO', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000005', 'DIS005', 'MYOCARDIAL_INFARCTION', 'CARDIO', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000006', 'DIS006', 'STROKE', 'BRAIN', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000007', 'DIS007', 'CANCER', 'ONCOLOGY', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000008', 'DIS008', 'THYROID_CANCER', 'ONCOLOGY', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000009', 'DIS009', 'LIVER_DISEASE', 'HEPATIC', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000010', 'DIS010', 'KIDNEY_DISEASE', 'RENAL', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000011', 'DIS011', 'DEPRESSION', 'MENTAL', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000012', 'DIS012', 'ANXIETY_DISORDER', 'MENTAL', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000013', 'DIS013', 'DISC_HERNIA', 'ORTHOPEDIC', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000014', 'DIS014', 'ASTHMA', 'RESPIRATORY', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID),
    ('53000000-0000-0000-0000-000000000015', 'DIS015', 'THYROID_DISORDER', 'ENDOCRINE', TRUE, @SYSTEM_USER_ID, @SYSTEM_USER_ID);

INSERT INTO customers (
    id,
    customer_code,
    customer_fp_id,
    customer_status,
    customer_grade,
    interest_yn,
    interest_reason,
    customer_name,
    customer_gender,
    customer_birth_date,
    customer_phone,
    customer_email,
    customer_zipcode,
    customer_address_road,
    customer_address_detail,
    customer_job,
    customer_company_name,
    customer_annual_income,
    customer_asset_size,
    customer_debt_status,
    customer_is_smoker,
    customer_is_drinker,
    customer_marital_status,
    customer_dependents_count,
    created_by,
    updated_by
)
SELECT
    CONCAT('40000000-0000-0000-0000-', LPAD(customer_no, 12, '0')) AS id,
    CONCAT('CUS-', customer_no) AS customer_code,
    customer_fp_id,
    CASE
        WHEN MOD(customer_no * 7 + 3, 10) < 7 THEN 'CONTRACTED'
        ELSE 'PROSPECT'
    END AS customer_status,
    CASE
        WHEN customer_no <= 75 THEN 'VIP'
        WHEN customer_no <= 300 THEN 'GOLD'
        ELSE 'GENERAL'
    END AS customer_grade,
    FALSE AS interest_yn,
    NULL AS interest_reason,
    CONCAT(
        CASE MOD(customer_no, 12)
            WHEN 0 THEN '김'
            WHEN 1 THEN '이'
            WHEN 2 THEN '박'
            WHEN 3 THEN '최'
            WHEN 4 THEN '정'
            WHEN 5 THEN '강'
            WHEN 6 THEN '조'
            WHEN 7 THEN '윤'
            WHEN 8 THEN '장'
            WHEN 9 THEN '임'
            WHEN 10 THEN '한'
            ELSE '오'
        END,
        CASE MOD(customer_no, 15)
            WHEN 0 THEN '민'
            WHEN 1 THEN '서'
            WHEN 2 THEN '지'
            WHEN 3 THEN '도'
            WHEN 4 THEN '하'
            WHEN 5 THEN '주'
            WHEN 6 THEN '현'
            WHEN 7 THEN '시'
            WHEN 8 THEN '유'
            WHEN 9 THEN '은'
            WHEN 10 THEN '태'
            WHEN 11 THEN '수'
            WHEN 12 THEN '재'
            WHEN 13 THEN '가'
            ELSE '다'
        END,
        CASE MOD(customer_no + 7, 15)
            WHEN 0 THEN '준'
            WHEN 1 THEN '연'
            WHEN 2 THEN '우'
            WHEN 3 THEN '민'
            WHEN 4 THEN '아'
            WHEN 5 THEN '윤'
            WHEN 6 THEN '호'
            WHEN 7 THEN '현'
            WHEN 8 THEN '진'
            WHEN 9 THEN '원'
            WHEN 10 THEN '림'
            WHEN 11 THEN '혁'
            WHEN 12 THEN '서'
            WHEN 13 THEN '빈'
            ELSE '율'
        END
    ) AS customer_name,
    CASE
        WHEN MOD(customer_no, 2) = 0 THEN 'FEMALE'
        ELSE 'MALE'
    END AS customer_gender,
    DATE_ADD('1975-01-01', INTERVAL MOD(customer_no * 37, 11000) DAY) AS customer_birth_date,
    CONCAT('010-5', LPAD(customer_no, 7, '0')) AS customer_phone,
    CONCAT('customer', LPAD(customer_no, 4, '0'), '@relia.com') AS customer_email,
    LPAD(10000 + MOD(customer_no, 90000), 5, '0') AS customer_zipcode,
    CONCAT(
        CASE MOD(customer_no, 10)
            WHEN 0 THEN '서울특별시 강남구 테헤란로 '
            WHEN 1 THEN '서울특별시 서초구 강남대로 '
            WHEN 2 THEN '서울특별시 송파구 송파대로 '
            WHEN 3 THEN '서울특별시 마포구 월드컵북로 '
            WHEN 4 THEN '서울특별시 영등포구 국제금융로 '
            WHEN 5 THEN '서울특별시 용산구 한강대로 '
            WHEN 6 THEN '서울특별시 은평구 통일로 '
            WHEN 7 THEN '서울특별시 노원구 동일로 '
            WHEN 8 THEN '서울특별시 구로구 디지털로 '
            ELSE '서울특별시 강서구 공항대로 '
        END,
        100 + MOD(customer_no, 150)
    ) AS customer_address_road,
    CONCAT(MOD(customer_no, 20) + 1, '층 ', MOD(customer_no, 12) + 1, '호') AS customer_address_detail,
    CASE MOD(customer_no, 6)
        WHEN 0 THEN '사무직'
        WHEN 1 THEN '교사'
        WHEN 2 THEN '간호사'
        WHEN 3 THEN '엔지니어'
        WHEN 4 THEN '회계사'
        ELSE '자영업'
    END AS customer_job,
    CASE MOD(customer_no, 6)
        WHEN 0 THEN '리라이프파트너스'
        WHEN 1 THEN '서울교육지원'
        WHEN 2 THEN '라이프메디컬'
        WHEN 3 THEN '넥스트코어'
        WHEN 4 THEN '프라임파이낸스'
        ELSE '개인사업'
    END AS customer_company_name,
    40000000 + (MOD(customer_no, 12) * 5000000) AS customer_annual_income,
    100000000 + (MOD(customer_no, 20) * 20000000) AS customer_asset_size,
    CASE MOD(customer_no, 4)
        WHEN 0 THEN 'NONE'
        WHEN 1 THEN 'MORTGAGE'
        WHEN 2 THEN 'CREDIT_LOAN'
        ELSE 'BUSINESS_LOAN'
    END AS customer_debt_status,
    CASE
        WHEN MOD(customer_no, 7) IN (0, 3) THEN TRUE
        ELSE FALSE
    END AS customer_is_smoker,
    CASE
        WHEN MOD(customer_no, 5) IN (0, 2, 4) THEN TRUE
        ELSE FALSE
    END AS customer_is_drinker,
    CASE
        WHEN MOD(customer_no, 3) = 0 THEN 'MARRIED'
        ELSE 'SINGLE'
    END AS customer_marital_status,
    CASE
        WHEN MOD(customer_no, 3) = 0 THEN MOD(customer_no, 3) + 1
        ELSE 0
    END AS customer_dependents_count,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY fp_no, slot_no) AS customer_no,
        fp_id AS customer_fp_id
    FROM (
        SELECT
            numbers.n AS fp_no,
            CONCAT('30000000-0000-0000-0000-', LPAD(numbers.n + 21, 12, '0')) AS fp_id,
            CASE
                WHEN numbers.n BETWEEN 1 AND 15 THEN 5
                WHEN numbers.n BETWEEN 16 AND 30 THEN 6
                WHEN numbers.n BETWEEN 31 AND 50 THEN 7
                WHEN numbers.n BETWEEN 51 AND 70 THEN 8
                WHEN numbers.n BETWEEN 71 AND 85 THEN 9
                ELSE 10
            END AS customer_quota
        FROM (
            SELECT ones.n + (tens.n * 10) + 1 AS n
            FROM (
                SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
            ) ones
            CROSS JOIN (
                SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
            ) tens
            WHERE ones.n + (tens.n * 10) < 100
        ) numbers
    ) fp
    JOIN (
        SELECT 1 AS slot_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
        UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    ) slots
        ON slots.slot_no <= fp.customer_quota
) customer_seed;

ALTER SEQUENCE customer_code_seq RESTART WITH 751;

INSERT INTO customer_underlying_diseases (
    id,
    customer_id,
    disease_code,
    created_by,
    updated_by
)
SELECT
    CONCAT('54000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    CONCAT('40000000-0000-0000-0000-', LPAD(customer_no, 12, '0')) AS customer_id,
    CONCAT('DIS', LPAD(((seq_no - 1) MOD 15) + 1, 3, '0')) AS disease_code,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY customer_no, disease_slot) AS seq_no,
        customer_no
    FROM (
        SELECT customer_no, 1 AS disease_slot
        FROM (
            SELECT numbers.n AS customer_no
            FROM (
                SELECT ones.n + (tens.n * 10) + (hundreds.n * 100) + 1 AS n
                FROM (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                ) ones
                CROSS JOIN (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                ) tens
                CROSS JOIN (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                ) hundreds
                WHERE ones.n + (tens.n * 10) + (hundreds.n * 100) < 750
            ) numbers
            WHERE MOD(numbers.n, 5) = 0
        ) primary_disease
        UNION ALL
        SELECT customer_no, 2 AS disease_slot
        FROM (
            SELECT numbers.n AS customer_no
            FROM (
                SELECT ones.n + (tens.n * 10) + (hundreds.n * 100) + 1 AS n
                FROM (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                ) ones
                CROSS JOIN (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                ) tens
                CROSS JOIN (
                    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                ) hundreds
                WHERE ones.n + (tens.n * 10) + (hundreds.n * 100) < 750
            ) numbers
            WHERE MOD(numbers.n, 20) = 0
        ) secondary_disease
    ) disease_seed
) final_disease_seed;

INSERT INTO contracts (
    id,
    contract_code,
    customer_id,
    fp_id,
    insurance_product_id,
    contract_date,
    contract_start_date,
    contract_end_date,
    contract_status,
    payment_period_years,
    payment_cycle,
    monthly_premium,
    coverage_start_date,
    coverage_end_date,
    coverage_summary,
    created_by,
    updated_by
)
SELECT
    CONCAT('60000000-0000-0000-0000-', LPAD(contract_no, 12, '0')) AS id,
    CONCAT('CTR', LPAD(contract_no, 6, '0')) AS contract_code,
    customer_id,
    fp_id,
    CONCAT('52000000-0000-0000-0000-', LPAD(MOD(contract_no - 1, 24) + 1, 12, '0')) AS insurance_product_id,
    DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY) AS contract_date,
    DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY) AS contract_start_date,
    CASE
        WHEN MOD(contract_no, 6) = 0 THEN DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 10 YEAR)
        WHEN MOD(contract_no, 5) = 0 THEN DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 15 YEAR)
        ELSE DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 20 YEAR)
    END AS contract_end_date,
    'MAINTENANCE' AS contract_status,
    CASE
        WHEN MOD(contract_no, 6) = 0 THEN 10
        WHEN MOD(contract_no, 5) = 0 THEN 15
        ELSE 20
    END AS payment_period_years,
    'MONTHLY',
    45000 + (MOD(contract_no, 18) * 7000) AS monthly_premium,
    DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY) AS coverage_start_date,
    CASE
        WHEN MOD(contract_no, 6) = 0 THEN DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 10 YEAR)
        WHEN MOD(contract_no, 5) = 0 THEN DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 15 YEAR)
        ELSE DATE_ADD(DATE_ADD('2022-01-01', INTERVAL MOD(contract_no * 11, 1000) DAY), INTERVAL 20 YEAR)
    END AS coverage_end_date,
    CONCAT('생명보험 보장 설계안 ', LPAD(contract_no, 6, '0')) AS coverage_summary,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY contracted_customer_rank, slot_no) AS contract_no,
        customer_id,
        fp_id,
        slot_no
    FROM (
        SELECT
            id AS customer_id,
            customer_fp_id AS fp_id,
            ROW_NUMBER() OVER (
                ORDER BY MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 37, 101),
                         MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 13, 53),
                         customer_code
            ) AS contracted_customer_rank,
            CASE
                WHEN ROW_NUMBER() OVER (
                    ORDER BY MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 37, 101),
                             MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 13, 53),
                             customer_code
                ) <= 260 THEN 1
                WHEN ROW_NUMBER() OVER (
                    ORDER BY MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 37, 101),
                             MOD(CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED) * 13, 53),
                             customer_code
                ) <= 440 THEN 2
                ELSE 3
            END AS contract_quota
        FROM customers
        WHERE customer_status = 'CONTRACTED'
    ) contracted_customers
    JOIN (
        SELECT 1 AS slot_no UNION ALL SELECT 2 UNION ALL SELECT 3
    ) slots
        ON slots.slot_no <= contracted_customers.contract_quota
) contract_seed;

-- Align a subset of active contracts to the demo reference month so that
-- interest customers are derived from actual contract / monthly closing data.
UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_ADD('2026-06-08', INTERVAL target.seq_no + 3 DAY) AS due_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        JOIN insurance_products ip ON ip.id = ct.insurance_product_id
        WHERE ct.contract_status = 'MAINTENANCE'
          AND ip.is_renewable = TRUE
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 211),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 29, 97),
                 ct.contract_code
        LIMIT 24
    ) target
) renewal_targets ON renewal_targets.contract_id = ct.id
SET ct.contract_end_date = renewal_targets.due_date,
    ct.coverage_end_date = renewal_targets.due_date,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_ADD('2026-06-08', INTERVAL target.seq_no + 11 DAY) AS due_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        JOIN insurance_products ip ON ip.id = ct.insurance_product_id
        WHERE ct.contract_status = 'MAINTENANCE'
          AND ip.is_renewable = FALSE
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 223),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 31, 89),
                 ct.contract_code
        LIMIT 18
    ) target
) maturity_targets ON maturity_targets.contract_id = ct.id
SET ct.contract_end_date = maturity_targets.due_date,
    ct.coverage_end_date = maturity_targets.due_date,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id,
        DATE_SUB('2026-06-08', INTERVAL target.seq_no + 12 DAY) AS completed_date
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
            ct.id AS contract_id
        FROM contracts ct
        JOIN (
            SELECT customer_id
            FROM contracts
            GROUP BY customer_id
            HAVING COUNT(*) = 1
        ) single_contract_customers ON single_contract_customers.customer_id = ct.customer_id
        LEFT JOIN (
            SELECT renewal_seed.contract_id
            FROM (
                SELECT ct.id AS contract_id
                FROM contracts ct
                JOIN insurance_products ip ON ip.id = ct.insurance_product_id
                WHERE ct.contract_status = 'MAINTENANCE'
                  AND ip.is_renewable = TRUE
                ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 211),
                         MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 29, 97),
                         ct.contract_code
                LIMIT 24
            ) renewal_seed
        ) renewal_targets ON renewal_targets.contract_id = ct.id
        LEFT JOIN (
            SELECT maturity_seed.contract_id
            FROM (
                SELECT ct.id AS contract_id
                FROM contracts ct
                JOIN insurance_products ip ON ip.id = ct.insurance_product_id
                WHERE ct.contract_status = 'MAINTENANCE'
                  AND ip.is_renewable = FALSE
                ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 19, 223),
                         MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 31, 89),
                         ct.contract_code
                LIMIT 18
            ) maturity_seed
        ) maturity_targets ON maturity_targets.contract_id = ct.id
        WHERE ct.contract_status = 'MAINTENANCE'
          AND renewal_targets.contract_id IS NULL
          AND maturity_targets.contract_id IS NULL
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 23, 227),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 41, 101),
                 ct.contract_code
        LIMIT 72
    ) target
) completed_targets ON completed_targets.contract_id = ct.id
SET ct.contract_end_date = completed_targets.completed_date,
    ct.coverage_end_date = completed_targets.completed_date,
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts
SET contract_status = CASE
        WHEN contract_end_date < '2026-06-08' THEN 'COMPLETED'
        ELSE 'MAINTENANCE'
    END,
    updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id
    FROM (
        SELECT
            ct.id AS contract_id
        FROM contracts ct
        JOIN (
            SELECT customer_id
            FROM contracts
            GROUP BY customer_id
            HAVING COUNT(*) = 1
        ) single_contract_customers ON single_contract_customers.customer_id = ct.customer_id
        WHERE ct.contract_status = 'MAINTENANCE'
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 43, 239) DESC,
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 11, 107) DESC,
                 ct.contract_code DESC
        LIMIT 45
    ) target
) terminated_targets ON terminated_targets.contract_id = ct.id
SET ct.contract_status = 'TERMINATED',
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE contracts ct
JOIN (
    SELECT
        target.contract_id
    FROM (
        SELECT
            ct.id AS contract_id
        FROM contracts ct
        WHERE ct.contract_status = 'MAINTENANCE'
        ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 47, 241),
                 MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 13, 109),
                 ct.contract_code
        LIMIT 28
    ) target
) lapsed_targets ON lapsed_targets.contract_id = ct.id
SET ct.contract_status = 'LAPSED',
    ct.updated_by = @SYSTEM_USER_ID;

UPDATE customers c
SET c.customer_status = CASE
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
              AND ct.contract_status IN ('MAINTENANCE', 'LAPSED')
        ) THEN 'CONTRACTED'
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
              AND ct.contract_status = 'COMPLETED'
        ) THEN 'COMPLETED'
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
              AND ct.contract_status = 'TERMINATED'
        ) THEN 'TERMINATED'
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
        ) THEN 'CONTRACTED'
        ELSE 'PROSPECT'
    END,
    c.updated_by = @SYSTEM_USER_ID;

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
        '60500000-0000-0000-0000-',
        LPAD(ROW_NUMBER() OVER (ORDER BY closing_seed.closing_month, closing_seed.contract_code), 12, '0')
    ) AS id,
    closing_seed.closing_month,
    closing_seed.contract_id,
    closing_seed.snapshot_contract_status,
    closing_seed.payment_status,
    closing_seed.scheduled_payment_round AS current_payment_round,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'MAINTENANCE'
            THEN closing_seed.scheduled_payment_round
        ELSE NULL
    END AS maintenance_round,
    CASE WHEN closing_seed.snapshot_contract_status = 'LAPSED' THEN TRUE ELSE FALSE END AS lapse_yn,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'LAPSED'
            THEN closing_seed.lapse_at
        ELSE NULL
    END AS lapse_at,
    CASE WHEN closing_seed.snapshot_contract_status = 'TERMINATED' THEN TRUE ELSE FALSE END AS terminated_yn,
    CASE
        WHEN closing_seed.snapshot_contract_status = 'TERMINATED'
            THEN closing_seed.terminated_at
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
            WHEN base_seed.snapshot_contract_status = 'MAINTENANCE'
                 AND base_seed.unpaid_installment_count IS NOT NULL
                 AND base_seed.month_seq >= 6 - base_seed.unpaid_installment_count
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
                     AND months.month_seq >= lapsed_targets.event_month_seq
                    THEN lapsed_targets.event_date
                ELSE NULL
            END AS lapse_at,
            CASE
                WHEN terminated_targets.event_month_seq IS NOT NULL
                     AND months.month_seq >= terminated_targets.event_month_seq
                    THEN terminated_targets.event_date
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
        JOIN (
            SELECT 1 AS month_seq, '2026-01' AS closing_month, DATE('2026-01-31') AS closing_date
            UNION ALL SELECT 2, '2026-02', DATE('2026-02-28')
            UNION ALL SELECT 3, '2026-03', DATE('2026-03-31')
            UNION ALL SELECT 4, '2026-04', DATE('2026-04-30')
            UNION ALL SELECT 5, '2026-05', DATE('2026-05-31')
        ) months
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
                WHERE ct.contract_status = 'MAINTENANCE'
                  AND ct.contract_code LIKE 'CTR%'
                ORDER BY MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 59, 251) DESC,
                         MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED) * 17, 113) DESC,
                         ct.contract_code DESC
                LIMIT 32
            ) unpaid_seed
        ) unpaid_targets ON unpaid_targets.contract_id = ct.id
        LEFT JOIN (
            SELECT
                target.contract_id,
                MOD(target.seq_no - 1, 4) + 2 AS event_month_seq,
                CASE MOD(target.seq_no - 1, 4)
                    WHEN 0 THEN DATE('2026-02-25')
                    WHEN 1 THEN DATE('2026-03-26')
                    WHEN 2 THEN DATE('2026-04-25')
                    ELSE DATE('2026-05-26')
                END AS event_date
            FROM (
                SELECT
                    ROW_NUMBER() OVER (ORDER BY ct.contract_code) AS seq_no,
                    ct.id AS contract_id
                FROM contracts ct
                WHERE ct.contract_status = 'LAPSED'
                  AND ct.contract_code LIKE 'CTR%'
            ) target
        ) lapsed_targets ON lapsed_targets.contract_id = ct.id
        LEFT JOIN (
            SELECT
                target.contract_id,
                MOD(target.seq_no - 1, 4) + 2 AS event_month_seq,
                CASE MOD(target.seq_no - 1, 4)
                    WHEN 0 THEN DATE('2026-02-27')
                    WHEN 1 THEN DATE('2026-03-28')
                    WHEN 2 THEN DATE('2026-04-27')
                    ELSE DATE('2026-05-28')
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
LEFT JOIN (
    SELECT
        ct.customer_id,
        MIN(
            CASE
                WHEN cmc.payment_status = 'UNPAID' THEN 1
                WHEN ip.is_renewable = TRUE
                     AND ct.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN '2026-06-08' AND DATE_ADD('2026-06-08', INTERVAL 30 DAY) THEN 2
                WHEN ip.is_renewable = FALSE
                     AND ct.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN '2026-06-08' AND DATE_ADD('2026-06-08', INTERVAL 30 DAY) THEN 3
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
        WHEN interest_targets.interest_priority < 9 THEN TRUE
        ELSE FALSE
    END,
    c.interest_reason = CASE interest_targets.interest_priority
        WHEN 1 THEN 'UNPAID'
        WHEN 2 THEN 'RENEWAL_DUE'
        WHEN 3 THEN 'MATURITY_DUE'
        ELSE NULL
    END,
    c.updated_by = @SYSTEM_USER_ID;

-- ----------------------------------------------------------------------------
-- customer / contract / consultation bulk seed sections
-- ----------------------------------------------------------------------------
-- 1. consultations: target about 1,500 rows with type-specific detail tables
-- 2. consultations: target about 1,500 rows with type-specific detail tables
-- 3. commission source tables / notifications / handover source tables
-- ----------------------------------------------------------------------------
-- current progress
-- - organizations inserted
-- - users inserted
-- - insurance master inserted
-- - disease master inserted
-- - customers inserted (750 rows)
-- - customer_underlying_diseases inserted (~187 rows)
-- - contracts inserted (~875 rows)
-- ----------------------------------------------------------------------------

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
    CONCAT('70000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    1 AS consultation_sequence,
    customer_id,
    fp_id,
    NULL AS contract_id,
    'NEW_CONTRACT',
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'VISIT'
        WHEN 1 THEN 'PHONE'
        ELSE 'MESSAGE'
    END AS consultation_channel,
    DATE_ADD('2026-01-01 09:00:00', INTERVAL seq_no DAY) AS consulted_at,
    CONCAT('생명보장 니즈 분석 상담 ', LPAD(seq_no, 4, '0')) AS special_note,
    CASE
        WHEN MOD(seq_no, 2) = 0 THEN DATE_ADD('2026-07-01 10:00:00', INTERVAL MOD(seq_no, 45) DAY)
        ELSE NULL
    END AS next_scheduled_at,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED)) AS seq_no,
        id AS customer_id,
        customer_fp_id AS fp_id
    FROM customers
    WHERE customer_status = 'PROSPECT'
    ORDER BY CAST(SUBSTRING_INDEX(customer_code, '-', -1) AS UNSIGNED)
    LIMIT 225
) new_consultation_seed;

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
    CONCAT('70000000-0000-0000-0000-', LPAD(seq_no + 225, 12, '0')) AS id,
    2 AS consultation_sequence,
    customer_id,
    fp_id,
    contract_id,
    'RENEWAL',
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'VISIT'
        WHEN 1 THEN 'PHONE'
        ELSE 'MESSAGE'
    END AS consultation_channel,
    DATE_ADD('2026-02-01 10:00:00', INTERVAL seq_no DAY) AS consulted_at,
    CONCAT('보험 갱신 보장 점검 상담 ', LPAD(seq_no, 4, '0')) AS special_note,
    CASE
        WHEN MOD(seq_no, 4) <> 0 THEN DATE_ADD('2026-08-01 09:30:00', INTERVAL MOD(seq_no, 60) DAY)
        ELSE NULL
    END AS next_scheduled_at,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY contract_code) AS seq_no,
        customer_id,
        fp_id,
        id AS contract_id
    FROM contracts
    ORDER BY contract_code
    LIMIT 600
) renewal_consultation_seed;

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
    CONCAT('70000000-0000-0000-0000-', LPAD(seq_no + 825, 12, '0')) AS id,
    3 AS consultation_sequence,
    customer_id,
    fp_id,
    contract_id,
    'CLAIM',
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'VISIT'
        WHEN 1 THEN 'PHONE'
        ELSE 'MESSAGE'
    END AS consultation_channel,
    DATE_ADD('2026-03-01 11:00:00', INTERVAL seq_no DAY) AS consulted_at,
    CONCAT('보험금 청구 지원 상담 ', LPAD(seq_no, 4, '0')) AS special_note,
    CASE
        WHEN MOD(seq_no, 5) = 0 THEN DATE_ADD('2026-09-01 14:00:00', INTERVAL MOD(seq_no, 40) DAY)
        ELSE NULL
    END AS next_scheduled_at,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY contract_code DESC) AS seq_no,
        customer_id,
        fp_id,
        id AS contract_id
    FROM contracts
    ORDER BY contract_code DESC
    LIMIT 450
) claim_consultation_seed;

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
    CONCAT('70000000-0000-0000-0000-', LPAD(seq_no + 1275, 12, '0')) AS id,
    4 AS consultation_sequence,
    customer_id,
    fp_id,
    contract_id,
    'TERMINATION',
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'VISIT'
        WHEN 1 THEN 'PHONE'
        ELSE 'MESSAGE'
    END AS consultation_channel,
    DATE_ADD('2026-04-01 13:00:00', INTERVAL seq_no DAY) AS consulted_at,
    CONCAT('해지 방어 및 유지 설득 상담 ', LPAD(seq_no, 4, '0')) AS special_note,
    NULL AS next_scheduled_at,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY limited_contracts.contract_code) AS seq_no,
        limited_contracts.customer_id,
        limited_contracts.fp_id,
        limited_contracts.contract_id
    FROM (
        SELECT
            customer_id,
            fp_id,
            id AS contract_id,
            contract_code
        FROM contracts
        ORDER BY contract_code
        LIMIT 225 OFFSET 325
    ) limited_contracts
) termination_consultation_seed;

UPDATE consultations cs
JOIN (
    SELECT
        ranked.id,
        ROW_NUMBER() OVER (
            PARTITION BY ranked.customer_id
            ORDER BY ranked.consulted_at, ranked.created_at, ranked.id
        ) AS consultation_sequence
    FROM (
        SELECT
            id,
            customer_id,
            consulted_at,
            created_at
        FROM consultations
    ) ranked
) resequenced ON resequenced.id = cs.id
SET cs.consultation_sequence = resequenced.consultation_sequence,
    cs.updated_by = @SYSTEM_USER_ID;

UPDATE consultations cs
JOIN customers c ON c.id = cs.customer_id
SET cs.next_scheduled_at = NULL,
    cs.updated_by = @SYSTEM_USER_ID
WHERE c.customer_status IN ('COMPLETED', 'TERMINATED')
  AND cs.next_scheduled_at IS NOT NULL;

INSERT INTO consultation_new_details (
    id,
    consultation_id,
    monthly_income,
    has_existing_insurance,
    monthly_insurance_premium,
    existing_insurance_note,
    insurance_priority,
    created_by,
    updated_by
)
SELECT
    CONCAT('71000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    consultation_id,
    customer_annual_income / 12,
    CASE WHEN MOD(seq_no, 3) = 0 THEN FALSE ELSE TRUE END,
    CASE WHEN MOD(seq_no, 3) = 0 THEN NULL ELSE 80000 + (MOD(seq_no, 8) * 10000) END,
    CASE
        WHEN MOD(seq_no, 3) = 0 THEN NULL
        ELSE CONCAT('기존 생명보험 보장 점검 ', LPAD(seq_no, 4, '0'))
    END,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '사망보장'
        WHEN 1 THEN '암보장'
        WHEN 2 THEN '뇌심장질환보장'
        ELSE '노후준비'
    END AS insurance_priority,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY c.id) AS seq_no,
        cs.id AS consultation_id,
        c.customer_annual_income
    FROM consultations cs
    JOIN customers c ON c.id = cs.customer_id
    WHERE cs.consultation_type = 'NEW_CONTRACT'
) new_detail_seed;

INSERT INTO consultation_renewal_details (
    id,
    consultation_id,
    renewal_reason,
    renewal_scheduled_date,
    current_premium,
    renewal_premium,
    premium_change_rate,
    coverage_change_type,
    coverage_change_detail,
    customer_reaction,
    consultation_result,
    created_by,
    updated_by
)
SELECT
    CONCAT('73000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    consultation_id,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '연령증가'
        WHEN 1 THEN '특약재점검'
        WHEN 2 THEN '만기대비'
        ELSE '보험료조정'
    END AS renewal_reason,
    DATE_ADD(DATE(consulted_at), INTERVAL 30 DAY) AS renewal_scheduled_date,
    monthly_premium AS current_premium,
    ROUND(monthly_premium * (1 + ((MOD(seq_no, 5) * 0.03))), 2) AS renewal_premium,
    ROUND((MOD(seq_no, 5) * 3.00), 2) AS premium_change_rate,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'SAME'
        WHEN 1 THEN 'EXPAND'
        ELSE 'REDUCE'
    END AS coverage_change_type,
    CONCAT('생명보험 갱신 보장 내용 점검 ', LPAD(seq_no, 4, '0')) AS coverage_change_detail,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN '긍정적'
        WHEN 1 THEN '보통'
        ELSE '신중함'
    END AS customer_reaction,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '갱신확정'
        WHEN 1 THEN '추가상담필요'
        WHEN 2 THEN '상품비교중'
        ELSE '결정보류'
    END AS consultation_result,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY cs.id) AS seq_no,
        cs.id AS consultation_id,
        cs.consulted_at,
        ct.monthly_premium
    FROM consultations cs
    JOIN contracts ct ON ct.id = cs.contract_id
    WHERE cs.consultation_type = 'RENEWAL'
) renewal_detail_seed;

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
    CONCAT('74000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    consultation_id,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '서류안내'
        WHEN 1 THEN '심사진행중'
        WHEN 2 THEN '보완서류요청'
        ELSE '지급확정'
    END AS claim_stage,
    DATE_SUB(DATE(consulted_at), INTERVAL MOD(seq_no, 45) DAY) AS claim_event_date,
    CONCAT('생명보험 보험금 청구 지원 건 ', LPAD(seq_no, 4, '0')) AS claim_reason_detail,
    CONCAT('리라이프 메디컬센터 ', MOD(seq_no, 12) + 1) AS hospital_name,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '암 치료'
        WHEN 1 THEN '심장질환 치료'
        WHEN 2 THEN '뇌혈관질환 치료'
        ELSE '수술 및 입원 치료'
    END AS diagnosis_or_treatment,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN '외래'
        WHEN 1 THEN '입원'
        ELSE '퇴원'
    END AS hospitalization_status,
    CASE MOD(seq_no, 2)
        WHEN 0 THEN '없음'
        ELSE '완료'
    END AS surgery_status,
    CASE MOD(seq_no, 4)
        WHEN 0 THEN '접수완료'
        WHEN 1 THEN '심사중'
        WHEN 2 THEN '서류보완'
        ELSE '지급완료'
    END AS claim_result,
    CONCAT('보험금 청구 절차 안내 ', LPAD(seq_no, 4, '0')) AS guidance_summary,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY cs.id) AS seq_no,
        cs.id AS consultation_id,
        cs.consulted_at
    FROM consultations cs
    WHERE cs.consultation_type = 'CLAIM'
) claim_detail_seed;

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
    CONCAT('72000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    consultation_id,
    CASE WHEN MOD(seq_no, 2) = 0 THEN TRUE ELSE FALSE END AS premium_burden,
    CASE WHEN MOD(seq_no, 3) = 0 THEN TRUE ELSE FALSE END AS renewal_premium_burden,
    CASE WHEN MOD(seq_no, 5) = 0 THEN TRUE ELSE FALSE END AS payment_difficulty,
    CASE WHEN MOD(seq_no, 4) = 0 THEN TRUE ELSE FALSE END AS coverage_dissatisfaction,
    CASE WHEN MOD(seq_no, 6) = 0 THEN TRUE ELSE FALSE END AS duplicate_insurance,
    CASE WHEN MOD(seq_no, 3) IN (1, 2) THEN TRUE ELSE FALSE END AS product_remodeling_review,
    CASE WHEN MOD(seq_no, 7) = 0 THEN TRUE ELSE FALSE END AS comparing_other_company,
    CASE WHEN MOD(seq_no, 8) = 0 THEN TRUE ELSE FALSE END AS moving_to_other_company,
    CASE WHEN MOD(seq_no, 9) = 0 THEN TRUE ELSE FALSE END AS planner_contact_dissatisfaction,
    CASE WHEN MOD(seq_no, 10) = 0 THEN TRUE ELSE FALSE END AS management_dissatisfaction,
    CASE MOD(seq_no, 3)
        WHEN 0 THEN 'HIGH'
        WHEN 1 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS retention_possibility,
    @SYSTEM_USER_ID,
    @SYSTEM_USER_ID
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY cs.id) AS seq_no,
        cs.id AS consultation_id
    FROM consultations cs
    WHERE cs.consultation_type = 'TERMINATION'
) cancel_detail_seed;

INSERT INTO handover_requests (
    id,
    customer_id,
    current_fp_id,
    request_type,
    request_status,
    created_by,
    updated_by
)
SELECT
    CONCAT('80000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    customer_id,
    current_fp_id,
    CASE
        WHEN MOD(seq_no, 4) = 0 THEN 'RESIGNATION'
        ELSE 'VOLUNTARY'
    END AS request_type,
    CASE
        WHEN seq_no <= 30 THEN 'COMPLETED'
        WHEN seq_no <= 40 THEN 'MANAGER_PENDING'
        ELSE 'RETRY'
    END AS request_status,
    current_fp_id,
    current_fp_id
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED)) AS seq_no,
        c.id AS customer_id,
        c.customer_fp_id AS current_fp_id
    FROM customers c
    WHERE c.customer_status = 'CONTRACTED'
    ORDER BY CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED)
    LIMIT 50
) handover_request_seed;

INSERT INTO handover_recommendations (
    id,
    handover_request_id,
    recommended_fp_id,
    recommended_fp_name,
    recommendation_reason,
    approval_status,
    reviewed_by,
    approved_at,
    rejected_at,
    rejection_reason
)
SELECT
    CONCAT('81000000-0000-0000-0000-', LPAD(rec_seq_no, 12, '0')) AS id,
    handover_request_id,
    recommended_fp_id,
    recommended_fp_name,
    recommendation_reason,
    approval_status,
    reviewed_by,
    approved_at,
    rejected_at,
    rejection_reason
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY hrs.seq_no, slot_no) AS rec_seq_no,
        hrs.handover_request_id,
        rec_user.id AS recommended_fp_id,
        rec_user.user_name AS recommended_fp_name,
        CASE
            WHEN slot_no = 1 THEN '동일 지점 내 고객군 유사성과 인수 여력을 반영한 추천'
            ELSE '지점 내 업무 분산과 계약 유지율을 고려한 차순위 추천'
        END AS recommendation_reason,
        CASE
            WHEN hrs.seq_no <= 30 AND slot_no = 1 THEN 'APPROVED'
            WHEN hrs.seq_no BETWEEN 31 AND 40 AND slot_no = 1 THEN 'PENDING'
            WHEN hrs.seq_no > 40 AND slot_no = 1 THEN 'REJECTED'
            ELSE 'PENDING'
        END AS approval_status,
        bm.id AS reviewed_by,
        CASE
            WHEN hrs.seq_no <= 30 AND slot_no = 1 THEN DATE_ADD('2026-05-01 10:00:00', INTERVAL hrs.seq_no DAY)
            ELSE NULL
        END AS approved_at,
        CASE
            WHEN hrs.seq_no > 40 AND slot_no = 1 THEN DATE_ADD('2026-05-20 15:00:00', INTERVAL hrs.seq_no DAY)
            ELSE NULL
        END AS rejected_at,
        CASE
            WHEN hrs.seq_no > 40 AND slot_no = 1 THEN '고객 적합도 검토 결과 추가 검토 필요'
            ELSE NULL
        END AS rejection_reason
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY hr.id) AS seq_no,
            hr.id AS handover_request_id,
            current_fp.id AS current_fp_id,
            current_fp.organization_id AS organization_id,
            CAST(SUBSTRING(current_fp.emp_code, 3) AS UNSIGNED) AS current_fp_no
        FROM handover_requests hr
        JOIN users current_fp ON current_fp.id = hr.current_fp_id
    ) hrs
    JOIN (
        SELECT 1 AS slot_no UNION ALL SELECT 2
    ) slots
    JOIN users bm
        ON bm.user_role = 'BRANCH_MANAGER'
       AND bm.organization_id = hrs.organization_id
    JOIN users rec_user
        ON rec_user.user_role = 'FP'
       AND rec_user.organization_id = hrs.organization_id
       AND rec_user.emp_code = CONCAT(
            'FP',
            LPAD(
                CASE
                    WHEN slot_no = 1 THEN
                        CASE
                            WHEN MOD(hrs.current_fp_no, 10) = 0 THEN hrs.current_fp_no - 9
                            ELSE hrs.current_fp_no + 1
                        END
                    ELSE
                        CASE
                            WHEN MOD(hrs.current_fp_no, 10) IN (0, 9) THEN hrs.current_fp_no - 8
                            ELSE hrs.current_fp_no + 2
                        END
                END,
                3,
                '0'
            )
       )
) recommendation_seed;

INSERT INTO customer_fp_history (
    id,
    customer_fp_sequence,
    customer_id,
    handover_request_id,
    before_fp_id,
    before_fp_name,
    after_fp_id,
    after_fp_name,
    changed_reason,
    changed_at,
    changed_by
)
SELECT
    CONCAT('82000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
    1 AS customer_fp_sequence,
    customer_id,
    handover_request_id,
    before_fp_id,
    before_fp_name,
    after_fp_id,
    after_fp_name,
    '지점장 승인에 따른 담당 FP 변경',
    approved_at,
    reviewed_by
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY hr.id) AS seq_no,
        hr.customer_id,
        hr.id AS handover_request_id,
        rec.recommended_fp_id AS before_fp_id,
        rec.recommended_fp_name AS before_fp_name,
        c.customer_fp_id AS after_fp_id,
        after_fp.user_name AS after_fp_name,
        rec.approved_at,
        rec.reviewed_by
    FROM handover_requests hr
    JOIN customers c ON c.id = hr.customer_id
    JOIN handover_recommendations rec
        ON rec.handover_request_id = hr.id
       AND rec.approval_status = 'APPROVED'
    JOIN users after_fp ON after_fp.id = c.customer_fp_id
    WHERE hr.request_status = 'COMPLETED'
) history_seed;

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
    CONCAT('90000000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (
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
            WHEN cmc.current_payment_round <= 24 THEN 'INITIAL'
            ELSE 'MAINTENANCE'
        END AS commission_type,
        CASE
            WHEN cmc.current_payment_round <= 24 THEN ROUND(
                cmc.monthly_premium * (
                    CASE ct.payment_period_years
                        WHEN 10 THEN 4.80
                        WHEN 15 THEN 5.40
                        ELSE 6.00
                    END
                    + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 3) * 0.30)
                ),
                2
            )
            ELSE ROUND(
                cmc.monthly_premium * (
                    CASE ct.payment_period_years
                        WHEN 10 THEN 0.90
                        WHEN 15 THEN 1.00
                        ELSE 1.10
                    END
                    + (MOD(CAST(RIGHT(ct.contract_code, 6) AS UNSIGNED), 2) * 0.10)
                ),
                2
            )
        END AS gross_commission_amount
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
        CASE cmc.contract_status
            WHEN 'TERMINATED' THEN ROUND(ct.monthly_premium * 1.20, 2)
            ELSE ROUND(ct.monthly_premium * 0.80, 2)
        END AS gross_commission_amount
    FROM contract_monthly_closing cmc
    JOIN contracts ct ON ct.id = cmc.contract_id
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    WHERE ((
            cmc.contract_status = 'LAPSED'
        AND cmc.lapse_yn = TRUE
        AND DATE_FORMAT(cmc.lapse_at, '%Y-%m') = cmc.closing_month
    ) OR (
            cmc.contract_status = 'TERMINATED'
        AND cmc.terminated_yn = TRUE
        AND DATE_FORMAT(cmc.terminated_at, '%Y-%m') = cmc.closing_month
    ))
      AND ct.contract_code LIKE 'CTR%'
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
    CONCAT('91000000-0000-0000-0000-', LPAD(seq_no, 12, '0')) AS id,
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
        CAST(55 + (MOD(CAST(RIGHT(fp.emp_code, 3) AS UNSIGNED) - 1, 5) * 5) AS DECIMAL(5,2)) AS fp_payment_rate
    FROM gross_commission_records gcr
    JOIN contracts ct ON ct.id = gcr.contract_id
    JOIN users fp ON fp.id = ct.fp_id
) payment_seed;

-- ----------------------------------------------------------------------------
-- customer / contract / consultation bulk seed sections
-- ----------------------------------------------------------------------------
-- current progress
-- - organizations inserted
-- - users inserted
-- - insurance master inserted
-- - disease master inserted
-- - customers inserted (750 rows)
-- - customer_underlying_diseases inserted (~187 rows)
-- - contracts inserted (~875 rows)
-- - consultations inserted (1,500 rows)
--   - NEW_CONTRACT: 225 rows, PROSPECT only
--   - RENEWAL: 600 rows
--   - CLAIM: 450 rows
--   - TERMINATION: 225 rows
-- - consultation_new_details inserted
-- - consultation_renewal_details inserted
-- - consultation_claim_details inserted
-- - consultation_cancel_details inserted
-- - handover_requests inserted (50 rows)
-- - handover_recommendations inserted (100 rows)
-- - customer_fp_history inserted (30 rows)
-- - gross_commission_records inserted from monthly closing + recovery events
-- - payment_commission_records inserted 1:1 from gross commission records
-- ----------------------------------------------------------------------------
