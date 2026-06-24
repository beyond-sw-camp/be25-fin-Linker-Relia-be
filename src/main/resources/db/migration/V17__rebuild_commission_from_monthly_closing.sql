SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';

DELETE FROM branch_income_commission_monthly_closing;
DELETE FROM income_commission_monthly_closing;
DELETE FROM branch_commission_monthly_closing;
DELETE FROM fp_commission_monthly_closing;
DELETE FROM payment_commission_records;
DELETE FROM gross_commission_records;

DROP TEMPORARY TABLE IF EXISTS tmp_v17_paid_commission_source;
CREATE TEMPORARY TABLE tmp_v17_paid_commission_source (
    commission_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    contract_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    fp_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    organization_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    insurance_company_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    insurance_product_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    commission_type VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    gross_commission_amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (commission_month, contract_id, commission_type)
);

INSERT INTO tmp_v17_paid_commission_source (
    commission_month,
    contract_id,
    fp_id,
    organization_id,
    insurance_company_id,
    insurance_product_id,
    commission_type,
    gross_commission_amount
)
SELECT
    paid_rows.closing_month,
    paid_rows.contract_id,
    paid_rows.fp_id,
    paid_rows.organization_id,
    paid_rows.insurance_company_id,
    paid_rows.insurance_product_id,
    CASE
        WHEN paid_rows.current_payment_round = 1 THEN 'INITIAL'
        ELSE 'MAINTENANCE'
    END AS commission_type,
    ROUND(
        paid_rows.monthly_premium * CASE
            WHEN paid_rows.current_payment_round = 1 THEN 10
            ELSE 2
        END,
        2
    ) AS gross_commission_amount
FROM (
    SELECT
        cmc.closing_month,
        cmc.contract_id,
        ct.fp_id,
        fp.organization_id,
        ip.insurance_company_id,
        ip.id AS insurance_product_id,
        cmc.monthly_premium,
        cmc.current_payment_round
    FROM contract_monthly_closing cmc
    JOIN contracts ct ON ct.id = cmc.contract_id
    JOIN users fp ON fp.id = ct.fp_id
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    WHERE cmc.payment_status = 'PAID'
      AND cmc.contract_status = 'MAINTENANCE'
) paid_rows;

DROP TEMPORARY TABLE IF EXISTS tmp_v17_recovery_commission_source;
CREATE TEMPORARY TABLE tmp_v17_recovery_commission_source (
    commission_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    contract_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    fp_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    organization_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    insurance_company_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    insurance_product_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    gross_commission_amount DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (commission_month, contract_id)
);

INSERT INTO tmp_v17_recovery_commission_source (
    commission_month,
    contract_id,
    fp_id,
    organization_id,
    insurance_company_id,
    insurance_product_id,
    gross_commission_amount
)
SELECT
    recovery_targets.closing_month,
    recovery_targets.contract_id,
    recovery_targets.fp_id,
    recovery_targets.organization_id,
    recovery_targets.insurance_company_id,
    recovery_targets.insurance_product_id,
    ROUND(recovery_targets.paid_gross_amount, 2) AS gross_commission_amount
FROM (
    SELECT
        cmc.closing_month,
        cmc.contract_id,
        ct.fp_id,
        fp.organization_id,
        ip.insurance_company_id,
        ip.id AS insurance_product_id,
        COALESCE(SUM(prev_paid.gross_commission_amount), 0) AS paid_gross_amount
    FROM contract_monthly_closing cmc
    JOIN contracts ct ON ct.id = cmc.contract_id
    JOIN users fp ON fp.id = ct.fp_id
    JOIN insurance_products ip ON ip.id = ct.insurance_product_id
    LEFT JOIN tmp_v17_paid_commission_source prev_paid
        ON prev_paid.contract_id = cmc.contract_id
       AND prev_paid.commission_month COLLATE utf8mb4_unicode_ci
           < cmc.closing_month COLLATE utf8mb4_unicode_ci
    WHERE (
            cmc.contract_status = 'LAPSED'
        AND cmc.lapse_yn = TRUE
        AND cmc.lapse_at IS NOT NULL
        AND CONVERT(DATE_FORMAT(cmc.lapse_at, '%Y-%m') USING utf8mb4) COLLATE utf8mb4_unicode_ci
            = cmc.closing_month COLLATE utf8mb4_unicode_ci
    ) OR (
            cmc.contract_status = 'TERMINATED'
        AND cmc.terminated_yn = TRUE
        AND cmc.terminated_at IS NOT NULL
        AND CONVERT(DATE_FORMAT(cmc.terminated_at, '%Y-%m') USING utf8mb4) COLLATE utf8mb4_unicode_ci
            = cmc.closing_month COLLATE utf8mb4_unicode_ci
    )
    GROUP BY
        cmc.closing_month,
        cmc.contract_id,
        ct.fp_id,
        fp.organization_id,
        ip.insurance_company_id,
        ip.id
) recovery_targets
WHERE recovery_targets.paid_gross_amount > 0;

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
    CONCAT('95200000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY src.commission_month, src.contract_id, src.commission_type), 12, '0')),
    src.commission_month,
    src.contract_id,
    src.insurance_company_id,
    src.insurance_product_id,
    src.commission_type,
    src.gross_commission_amount,
    @SYSTEM_USER_ID
FROM (
    SELECT
        commission_month,
        contract_id,
        insurance_company_id,
        insurance_product_id,
        commission_type,
        gross_commission_amount
    FROM tmp_v17_paid_commission_source

    UNION ALL

    SELECT
        commission_month,
        contract_id,
        insurance_company_id,
        insurance_product_id,
        'RECOVERY' AS commission_type,
        gross_commission_amount
    FROM tmp_v17_recovery_commission_source
) src;

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
    CONCAT('95300000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gcr.commission_month, gcr.contract_id, gcr.commission_type), 12, '0')),
    gcr.commission_month,
    gcr.id,
    gcr.contract_id,
    ct.fp_id,
    fp.organization_id,
    gcr.insurance_company_id,
    gcr.insurance_product_id,
    CASE gcr.commission_type
        WHEN 'INITIAL' THEN 'INITIAL_PAYMENT'
        WHEN 'MAINTENANCE' THEN 'MAINTENANCE_PAYMENT'
        ELSE 'RECOVERY_COLLECTION'
    END AS commission_type,
    gcr.gross_commission_amount,
    70.00 AS fp_payment_rate,
    ROUND(gcr.gross_commission_amount * 0.7, 2) AS commission_amount,
    @SYSTEM_USER_ID
FROM gross_commission_records gcr
JOIN contracts ct ON ct.id = gcr.contract_id
JOIN users fp ON fp.id = ct.fp_id;

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
    CONCAT('95400000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY pcr.commission_month, pcr.fp_id), 12, '0')),
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
    CONCAT('95500000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY closing_month, organization_id), 12, '0')),
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
    CONCAT('95600000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gross_summary.closing_month), 12, '0')),
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
    CONCAT('95700000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY gross_summary.closing_month, gross_summary.organization_id), 12, '0')),
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
        fp.organization_id,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type IN ('INITIAL', 'MAINTENANCE') THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_gross_commission_amount,
        COUNT(DISTINCT gcr.contract_id) AS contract_count,
        COUNT(DISTINCT ct.fp_id) AS fp_count,
        TIMESTAMP(LAST_DAY(STR_TO_DATE(CONCAT(gcr.commission_month, '-01'), '%Y-%m-%d')), '18:00:00') AS closed_at
    FROM gross_commission_records gcr
    JOIN contracts ct ON ct.id = gcr.contract_id
    JOIN users fp ON fp.id = ct.fp_id
    GROUP BY gcr.commission_month, fp.organization_id
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

DROP TEMPORARY TABLE IF EXISTS tmp_v17_recovery_commission_source;
DROP TEMPORARY TABLE IF EXISTS tmp_v17_paid_commission_source;
