START TRANSACTION;

DELETE FROM all_branch_contract_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM branch_contract_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM branch_income_commission_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM income_commission_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM branch_commission_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM fp_commission_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM payment_commission_records
WHERE commission_month = '2026-06';

DELETE FROM gross_commission_records
WHERE commission_month = '2026-06';

DELETE FROM fp_monthly_performance_closing
WHERE closing_month = '2026-06';

DELETE FROM contract_monthly_closing
WHERE closing_month = '2026-06';

DROP TEMPORARY TABLE IF EXISTS tmp_v23_contract_monthly_closing_seed;
CREATE TEMPORARY TABLE tmp_v23_contract_monthly_closing_seed (
    scenario_code VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    contract_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    contract_status VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    payment_status VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    current_payment_round INT NULL,
    maintenance_round INT NULL,
    lapse_yn BOOLEAN NOT NULL,
    lapse_at DATE NULL,
    terminated_yn BOOLEAN NOT NULL,
    terminated_at DATE NULL,
    customer_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    fp_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    contract_date DATE NOT NULL,
    contract_start_date DATE NOT NULL,
    contract_end_date DATE NOT NULL,
    payment_period_years INT NOT NULL,
    payment_cycle VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    monthly_premium DECIMAL(15,2) NOT NULL,
    coverage_start_date DATE NOT NULL,
    coverage_end_date DATE NOT NULL,
    coverage_summary TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    closed_at DATETIME NOT NULL,
    PRIMARY KEY (contract_id)
);

INSERT INTO tmp_v23_contract_monthly_closing_seed (
    scenario_code,
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
    base.scenario_code,
    base.contract_id,
    base.contract_status,
    base.payment_status,
    base.current_payment_round,
    CASE
        WHEN base.contract_status = 'MAINTENANCE' AND base.payment_status = 'PAID'
            THEN base.current_payment_round
        WHEN base.contract_status IN ('MAINTENANCE', 'LAPSED')
            THEN COALESCE(
                base.prev_maintenance_round,
                base.prev_current_payment_round,
                GREATEST(1, base.current_payment_round - 1)
            )
        ELSE NULL
    END AS maintenance_round,
    CASE WHEN base.contract_status = 'LAPSED' THEN TRUE ELSE FALSE END AS lapse_yn,
    CASE
        WHEN base.contract_status = 'LAPSED'
            THEN CASE
                WHEN base.prev_contract_status = 'LAPSED' THEN base.prev_lapse_at
                ELSE DATE('2026-06-30')
            END
        ELSE NULL
    END AS lapse_at,
    CASE WHEN base.contract_status = 'TERMINATED' THEN TRUE ELSE FALSE END AS terminated_yn,
    CASE
        WHEN base.contract_status = 'TERMINATED'
            THEN CASE
                WHEN base.prev_contract_status = 'TERMINATED' THEN base.prev_terminated_at
                ELSE DATE('2026-06-30')
            END
        ELSE NULL
    END AS terminated_at,
    base.customer_id,
    base.fp_id,
    base.contract_date,
    base.contract_start_date,
    base.contract_end_date,
    base.payment_period_years,
    base.payment_cycle,
    base.monthly_premium,
    base.coverage_start_date,
    base.coverage_end_date,
    base.coverage_summary,
    TIMESTAMP('2026-06-30', '23:59:59')
FROM (
    SELECT
        CASE
            WHEN base_contract.is_new_contract = TRUE THEN 'NEW_CONTRACT'
            WHEN base_contract.contract_status = 'COMPLETED' THEN 'COMPLETED'
            WHEN base_contract.contract_status = 'TERMINATED' THEN 'TERMINATED'
            WHEN base_contract.contract_status = 'LAPSED' THEN 'LAPSED'
            WHEN base_contract.payment_status = 'UNPAID' THEN 'MAINTENANCE_UNPAID'
            ELSE 'MAINTENANCE_PAID'
        END AS scenario_code,
        base_contract.contract_id,
        base_contract.contract_status,
        base_contract.payment_status,
        base_contract.current_payment_round,
        base_contract.customer_id,
        base_contract.fp_id,
        base_contract.contract_date,
        base_contract.contract_start_date,
        base_contract.contract_end_date,
        base_contract.payment_period_years,
        base_contract.payment_cycle,
        base_contract.monthly_premium,
        base_contract.coverage_start_date,
        base_contract.coverage_end_date,
        base_contract.coverage_summary,
        base_contract.prev_contract_status,
        base_contract.prev_payment_status,
        base_contract.prev_current_payment_round,
        base_contract.prev_maintenance_round,
        base_contract.prev_lapse_at,
        base_contract.prev_terminated_at
    FROM (
        SELECT
            ct.id AS contract_id,
            CASE
                WHEN ct.contract_end_date < DATE('2026-06-30') THEN 'COMPLETED'
                ELSE ct.contract_status
            END AS contract_status,
            CASE
                WHEN prev_cmc.payment_status = 'UNPAID'
                     AND (
                        CASE
                            WHEN ct.contract_end_date < DATE('2026-06-30') THEN 'COMPLETED'
                            ELSE ct.contract_status
                        END
                     ) IN ('MAINTENANCE', 'LAPSED') THEN 'UNPAID'
                WHEN ct.contract_status = 'LAPSED' THEN 'UNPAID'
                ELSE 'PAID'
            END AS payment_status,
            LEAST(
                ct.payment_period_years * 12,
                GREATEST(1, TIMESTAMPDIFF(MONTH, ct.contract_start_date, DATE('2026-06-30')) + 1)
            ) AS current_payment_round,
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
            ct.coverage_summary,
            prev_cmc.contract_status AS prev_contract_status,
            prev_cmc.payment_status AS prev_payment_status,
            prev_cmc.current_payment_round AS prev_current_payment_round,
            prev_cmc.maintenance_round AS prev_maintenance_round,
            prev_cmc.lapse_at AS prev_lapse_at,
            prev_cmc.terminated_at AS prev_terminated_at,
            CASE
                WHEN prev_cmc.contract_id IS NULL THEN TRUE
                ELSE FALSE
            END AS is_new_contract
        FROM contracts ct
        LEFT JOIN contract_monthly_closing prev_cmc
            ON prev_cmc.contract_id = ct.id
           AND prev_cmc.closing_month = '2026-05'
        WHERE ct.deleted_at IS NULL
          AND ct.contract_start_date < DATE('2026-07-01')
          AND (
                prev_cmc.contract_id IS NOT NULL
                OR ct.contract_start_date >= DATE('2026-06-01')
          )
    ) base_contract
) base;

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
    UUID(),
    '2026-06',
    seed.contract_id,
    seed.contract_status,
    seed.payment_status,
    seed.current_payment_round,
    seed.maintenance_round,
    seed.lapse_yn,
    seed.lapse_at,
    seed.terminated_yn,
    seed.terminated_at,
    seed.customer_id,
    seed.fp_id,
    seed.contract_date,
    seed.contract_start_date,
    seed.contract_end_date,
    seed.payment_period_years,
    seed.payment_cycle,
    seed.monthly_premium,
    seed.coverage_start_date,
    seed.coverage_end_date,
    seed.coverage_summary,
    seed.closed_at
FROM tmp_v23_contract_monthly_closing_seed seed;

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
    UUID(),
    src.commission_month,
    src.contract_id,
    src.insurance_company_id,
    src.insurance_product_id,
    src.commission_type,
    src.gross_commission_amount,
    src.created_by
FROM (
    SELECT
        cmc.closing_month AS commission_month,
        cmc.contract_id,
        ip.insurance_company_id,
        ip.id AS insurance_product_id,
        CASE
            WHEN cmc.current_payment_round = 1 THEN 'INITIAL'
            ELSE 'MAINTENANCE'
        END AS commission_type,
        ROUND(
            cmc.monthly_premium * CASE
                WHEN cmc.current_payment_round = 1 THEN 10
                ELSE 2
            END,
            2
        ) AS gross_commission_amount,
        COALESCE(ct.updated_by, ct.created_by) AS created_by
    FROM contract_monthly_closing cmc
    JOIN contracts ct
        ON ct.id = cmc.contract_id
    JOIN insurance_products ip
        ON ip.id = ct.insurance_product_id
       AND ip.deleted_at IS NULL
    WHERE cmc.closing_month = '2026-06'
      AND cmc.payment_status = 'PAID'
      AND cmc.contract_status = 'MAINTENANCE'

    UNION ALL

    SELECT
        recovery_targets.closing_month AS commission_month,
        recovery_targets.contract_id,
        recovery_targets.insurance_company_id,
        recovery_targets.insurance_product_id,
        'RECOVERY' AS commission_type,
        ROUND(recovery_targets.paid_gross_amount, 2) AS gross_commission_amount,
        recovery_targets.created_by
    FROM (
        SELECT
            cmc.closing_month,
            cmc.contract_id,
            ip.insurance_company_id,
            ip.id AS insurance_product_id,
            COALESCE(SUM(
                CASE
                    WHEN prev_cmc.payment_status = 'PAID'
                        THEN prev_cmc.monthly_premium * CASE
                            WHEN prev_cmc.current_payment_round = 1 THEN 10
                            ELSE 2
                        END
                    ELSE 0
                END
            ), 0) AS paid_gross_amount,
            COALESCE(ct.updated_by, ct.created_by) AS created_by
        FROM contract_monthly_closing cmc
        JOIN contracts ct
            ON ct.id = cmc.contract_id
        JOIN insurance_products ip
            ON ip.id = ct.insurance_product_id
           AND ip.deleted_at IS NULL
        LEFT JOIN contract_monthly_closing prev_cmc
            ON prev_cmc.contract_id = cmc.contract_id
           AND prev_cmc.closing_month < cmc.closing_month
        WHERE cmc.closing_month = '2026-06'
          AND (
                (
                    cmc.contract_status = 'LAPSED'
                    AND cmc.lapse_yn = TRUE
                    AND cmc.lapse_at IS NOT NULL
                    AND DATE_FORMAT(cmc.lapse_at, '%Y-%m') = cmc.closing_month
                ) OR (
                    cmc.contract_status = 'TERMINATED'
                    AND cmc.terminated_yn = TRUE
                    AND cmc.terminated_at IS NOT NULL
                    AND DATE_FORMAT(cmc.terminated_at, '%Y-%m') = cmc.closing_month
                )
          )
        GROUP BY
            cmc.closing_month,
            cmc.contract_id,
            ip.insurance_company_id,
            ip.id,
            ct.updated_by,
            ct.created_by
    ) recovery_targets
    WHERE recovery_targets.paid_gross_amount > 0
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
    UUID(),
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
    END,
    gcr.gross_commission_amount,
    70.00,
    ROUND(gcr.gross_commission_amount * 0.7, 2),
    gcr.created_by
FROM gross_commission_records gcr
JOIN contracts ct
    ON ct.id = gcr.contract_id
JOIN users fp
    ON fp.id = ct.fp_id
   AND fp.deleted_at IS NULL
WHERE gcr.commission_month = '2026-06';

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
    UUID(),
    '2026-06',
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
    TIMESTAMP('2026-06-30', '23:59:59')
FROM payment_commission_records pcr
JOIN users fp
    ON fp.id = pcr.fp_id
   AND fp.deleted_at IS NULL
JOIN organizations org
    ON org.id = pcr.organization_id
   AND org.deleted_at IS NULL
WHERE pcr.commission_month = '2026-06'
GROUP BY pcr.fp_id, pcr.organization_id;

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
    UUID(),
    '2026-06',
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
    COUNT(DISTINCT pcr.fp_id),
    COUNT(DISTINCT pcr.contract_id),
    COUNT(DISTINCT CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.contract_id END),
    TIMESTAMP('2026-06-30', '23:59:59')
FROM payment_commission_records pcr
JOIN organizations org
    ON org.id = pcr.organization_id
   AND org.deleted_at IS NULL
WHERE pcr.commission_month = '2026-06'
GROUP BY pcr.organization_id;

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
    UUID(),
    '2026-06',
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
    TIMESTAMP('2026-06-30', '23:59:59')
FROM (
    SELECT
        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_initial_gross_commission_amount,
        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_maintenance_gross_commission_amount,
        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_insurance_recovery_amount
    FROM gross_commission_records gcr
    WHERE gcr.commission_month = '2026-06'
) gross_summary
CROSS JOIN (
    SELECT
        ROUND(COALESCE(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 0), 2) AS total_payment_commission_amount,
        ROUND(COALESCE(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 0), 2) AS total_fp_recovery_collection_amount
    FROM payment_commission_records pcr
    WHERE pcr.commission_month = '2026-06'
) payment_summary;

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
    UUID(),
    '2026-06',
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
    TIMESTAMP('2026-06-30', '23:59:59')
FROM (
    SELECT
        fp.organization_id,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_initial_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_maintenance_gross_commission_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_insurance_recovery_amount,
        ROUND(SUM(CASE WHEN gcr.commission_type IN ('INITIAL', 'MAINTENANCE') THEN gcr.gross_commission_amount ELSE 0 END), 2) AS total_gross_commission_amount,
        COUNT(DISTINCT gcr.contract_id) AS contract_count,
        COUNT(DISTINCT ct.fp_id) AS fp_count
    FROM gross_commission_records gcr
    JOIN contracts ct
        ON ct.id = gcr.contract_id
    JOIN users fp
        ON fp.id = ct.fp_id
       AND fp.deleted_at IS NULL
    JOIN organizations org
        ON org.id = fp.organization_id
       AND org.deleted_at IS NULL
    WHERE gcr.commission_month = '2026-06'
    GROUP BY fp.organization_id
) gross_summary
JOIN (
    SELECT
        pcr.organization_id,
        ROUND(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 2) AS total_payment_commission_amount,
        ROUND(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 2) AS total_fp_recovery_collection_amount
    FROM payment_commission_records pcr
    JOIN organizations org
        ON org.id = pcr.organization_id
       AND org.deleted_at IS NULL
    WHERE pcr.commission_month = '2026-06'
    GROUP BY pcr.organization_id
) payment_summary
    ON payment_summary.organization_id = gross_summary.organization_id;

INSERT INTO branch_contract_monthly_closing (
    id,
    closing_month,
    organization_id,
    insurance_company_id,
    insurance_category_id,
    total_contract_count,
    active_contract_count,
    completed_contract_count,
    terminated_contract_count,
    contract_success_rate,
    retention_rate,
    total_monthly_premium_amount,
    closed_at
)
SELECT
    UUID(),
    '2026-06',
    org.id,
    NULL,
    NULL,
    COALESCE(contract_summary.total_contract_count, 0),
    COALESCE(contract_summary.active_contract_count, 0),
    COALESCE(contract_summary.completed_contract_count, 0),
    COALESCE(contract_summary.terminated_contract_count, 0),
    CASE
        WHEN COALESCE(customer_summary.prospect_customer_count, 0) + COALESCE(customer_summary.contracted_customer_count, 0) = 0 THEN 0
        ELSE ROUND(
            COALESCE(customer_summary.contracted_customer_count, 0) * 100.0
            / (COALESCE(customer_summary.prospect_customer_count, 0) + COALESCE(customer_summary.contracted_customer_count, 0)),
            2
        )
    END,
    CASE
        WHEN COALESCE(contract_summary.total_contract_count, 0) = 0 THEN 0
        ELSE ROUND(
            COALESCE(contract_summary.active_contract_count, 0) * 100.0 / contract_summary.total_contract_count,
            2
        )
    END,
    COALESCE(contract_summary.total_monthly_premium_amount, 0),
    TIMESTAMP('2026-06-30', '23:59:59')
FROM organizations org
LEFT JOIN (
    SELECT
        fp.organization_id,
        COUNT(*) AS total_contract_count,
        SUM(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END) AS active_contract_count,
        SUM(CASE WHEN cmc.contract_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_contract_count,
        SUM(CASE WHEN cmc.contract_status IN ('LAPSED', 'TERMINATED') THEN 1 ELSE 0 END) AS terminated_contract_count,
        ROUND(COALESCE(SUM(cmc.monthly_premium), 0), 2) AS total_monthly_premium_amount
    FROM contract_monthly_closing cmc
    JOIN users fp
        ON fp.id = cmc.fp_id
       AND fp.deleted_at IS NULL
    WHERE cmc.closing_month = '2026-06'
    GROUP BY fp.organization_id
) contract_summary
    ON contract_summary.organization_id = org.id
LEFT JOIN (
    SELECT
        fp.organization_id,
        SUM(CASE WHEN c.customer_status = 'PROSPECT' THEN 1 ELSE 0 END) AS prospect_customer_count,
        SUM(CASE WHEN c.customer_status = 'CONTRACTED' THEN 1 ELSE 0 END) AS contracted_customer_count
    FROM customers c
    JOIN users fp
        ON fp.id = c.customer_fp_id
       AND fp.deleted_at IS NULL
    WHERE c.deleted_at IS NULL
    GROUP BY fp.organization_id
) customer_summary
    ON customer_summary.organization_id = org.id
WHERE org.deleted_at IS NULL
  AND org.organization_type = 'BRANCH';

INSERT INTO all_branch_contract_monthly_closing (
    id,
    closing_month,
    insurance_company_id,
    insurance_category_id,
    total_contract_count,
    active_contract_count,
    completed_contract_count,
    terminated_contract_count,
    contract_success_rate,
    retention_rate,
    total_monthly_premium_amount,
    closed_at
)
SELECT
    UUID(),
    '2026-06',
    NULL,
    NULL,
    COUNT(*),
    COALESCE(SUM(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END), 0),
    COALESCE(SUM(CASE WHEN cmc.contract_status = 'COMPLETED' THEN 1 ELSE 0 END), 0),
    COALESCE(SUM(CASE WHEN cmc.contract_status IN ('LAPSED', 'TERMINATED') THEN 1 ELSE 0 END), 0),
    CASE
        WHEN customer_summary.total_target_customer_count = 0 THEN 0
        ELSE ROUND(customer_summary.total_contracted_customer_count * 100.0 / customer_summary.total_target_customer_count, 2)
    END,
    CASE
        WHEN COUNT(*) = 0 THEN 0
        ELSE ROUND(COALESCE(SUM(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END), 0) * 100.0 / COUNT(*), 2)
    END,
    ROUND(COALESCE(SUM(cmc.monthly_premium), 0), 2),
    TIMESTAMP('2026-06-30', '23:59:59')
FROM contract_monthly_closing cmc
CROSS JOIN (
    SELECT
        COALESCE(SUM(CASE WHEN c.customer_status = 'CONTRACTED' THEN 1 ELSE 0 END), 0) AS total_contracted_customer_count,
        COALESCE(SUM(CASE WHEN c.customer_status IN ('PROSPECT', 'CONTRACTED') THEN 1 ELSE 0 END), 0) AS total_target_customer_count
    FROM customers c
    WHERE c.deleted_at IS NULL
) customer_summary
WHERE cmc.closing_month = '2026-06';

INSERT INTO fp_monthly_performance_closing (
    id,
    closing_month,
    fp_id,
    organization_id,
    new_contract_count,
    completed_contract_count,
    retention_rate,
    consultation_count,
    customer_count,
    new_handover_customer_count,
    commission_amount,
    performance_score,
    branch_rank,
    total_rank,
    closed_at
)
SELECT
    UUID(),
    ranked.closing_month,
    ranked.fp_id,
    ranked.organization_id,
    ranked.new_contract_count,
    ranked.completed_contract_count,
    ranked.retention_rate,
    ranked.consultation_count,
    ranked.customer_count,
    ranked.new_handover_customer_count,
    ranked.commission_amount,
    ranked.performance_score,
    ranked.branch_rank,
    ranked.total_rank,
    ranked.closed_at
FROM (
    SELECT
        base.closing_month,
        base.fp_id,
        base.organization_id,
        base.new_contract_count,
        base.completed_contract_count,
        base.retention_rate,
        base.consultation_count,
        base.customer_count,
        base.new_handover_customer_count,
        base.commission_amount,
        base.performance_score,
        ROW_NUMBER() OVER (
            PARTITION BY base.organization_id
            ORDER BY base.performance_score DESC, base.commission_amount DESC,
                     base.completed_contract_count DESC, base.fp_id ASC
        ) AS branch_rank,
        ROW_NUMBER() OVER (
            ORDER BY base.performance_score DESC, base.commission_amount DESC,
                     base.completed_contract_count DESC, base.fp_id ASC
        ) AS total_rank,
        base.closed_at
    FROM (
        SELECT
            '2026-06' AS closing_month,
            fp.id AS fp_id,
            fp.organization_id,
            COALESCE(new_contract_summary.new_contract_count, 0) AS new_contract_count,
            COALESCE(contract_summary.completed_contract_count, 0) AS completed_contract_count,
            CASE
                WHEN COALESCE(contract_summary.completed_contract_count, 0) = 0 THEN 0
                ELSE ROUND(
                    COALESCE(contract_summary.active_contract_count, 0) * 100.0
                    / contract_summary.completed_contract_count,
                    2
                )
            END AS retention_rate,
            COALESCE(consultation_summary.consultation_count, 0) AS consultation_count,
            COALESCE(customer_summary.customer_count, 0) AS customer_count,
            COALESCE(handover_summary.new_handover_customer_count, 0) AS new_handover_customer_count,
            COALESCE(commission_summary.commission_amount, 0) AS commission_amount,
            ROUND(
                COALESCE(commission_summary.commission_amount, 0) / 10000
                + LEAST(COALESCE(contract_summary.completed_contract_count, 0), 10) * 20
                + CASE
                    WHEN COALESCE(contract_summary.completed_contract_count, 0) = 0 THEN 0
                    ELSE ROUND(
                        COALESCE(contract_summary.active_contract_count, 0) * 100.0
                        / contract_summary.completed_contract_count,
                        2
                    )
                  END
                + LEAST(COALESCE(consultation_summary.consultation_count, 0), 20) * 5
                + LEAST(COALESCE(customer_summary.customer_count, 0), 20) * 10,
                2
            ) AS performance_score,
            TIMESTAMP('2026-06-30', '23:59:59') AS closed_at
        FROM users fp
        JOIN organizations org
            ON org.id = fp.organization_id
           AND org.deleted_at IS NULL
           AND org.organization_type = 'BRANCH'
        LEFT JOIN (
            SELECT
                ct.fp_id,
                COUNT(*) AS new_contract_count
            FROM contracts ct
            WHERE ct.deleted_at IS NULL
              AND ct.contract_date >= DATE('2026-06-01')
              AND ct.contract_date < DATE('2026-07-01')
            GROUP BY ct.fp_id
        ) new_contract_summary
            ON new_contract_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                ct.fp_id,
                COUNT(*) AS completed_contract_count,
                SUM(CASE WHEN ct.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END) AS active_contract_count
            FROM contracts ct
            WHERE ct.deleted_at IS NULL
            GROUP BY ct.fp_id
        ) contract_summary
            ON contract_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                c.fp_id,
                COUNT(*) AS consultation_count
            FROM consultations c
            WHERE c.deleted_at IS NULL
              AND c.consulted_at >= '2026-06-01 00:00:00'
              AND c.consulted_at < '2026-07-01 00:00:00'
            GROUP BY c.fp_id
        ) consultation_summary
            ON consultation_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                c.customer_fp_id AS fp_id,
                COUNT(*) AS customer_count
            FROM customers c
            WHERE c.deleted_at IS NULL
            GROUP BY c.customer_fp_id
        ) customer_summary
            ON customer_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                h.after_fp_id AS fp_id,
                COUNT(DISTINCT h.customer_id) AS new_handover_customer_count
            FROM customer_fp_history h
            WHERE h.changed_at >= '2026-06-01 00:00:00'
              AND h.changed_at < '2026-07-01 00:00:00'
            GROUP BY h.after_fp_id
        ) handover_summary
            ON handover_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                fcmc.fp_id,
                fcmc.net_commission_amount AS commission_amount
            FROM fp_commission_monthly_closing fcmc
            WHERE fcmc.closing_month = '2026-06'
        ) commission_summary
            ON commission_summary.fp_id = fp.id
        WHERE fp.deleted_at IS NULL
          AND fp.user_role = 'FP'
    ) base
) ranked;

DROP TEMPORARY TABLE IF EXISTS tmp_v23_contract_monthly_closing_seed;

COMMIT;
