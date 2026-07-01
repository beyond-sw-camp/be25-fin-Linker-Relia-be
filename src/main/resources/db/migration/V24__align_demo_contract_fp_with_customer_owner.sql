SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';

UPDATE contracts ct
JOIN customers c
    ON c.id = ct.customer_id
   AND c.deleted_at IS NULL
SET ct.fp_id = c.customer_fp_id,
    ct.updated_at = CURRENT_TIMESTAMP,
    ct.updated_by = @SYSTEM_USER_ID
WHERE ct.deleted_at IS NULL
  AND c.customer_fp_id IS NOT NULL
  AND ct.contract_code LIKE 'CTR%'
  AND ct.fp_id <> c.customer_fp_id;

UPDATE contract_monthly_closing cmc
JOIN contracts ct
    ON ct.id = cmc.contract_id
   AND ct.deleted_at IS NULL
JOIN customers c
    ON c.id = ct.customer_id
   AND c.deleted_at IS NULL
SET cmc.fp_id = c.customer_fp_id
WHERE c.customer_fp_id IS NOT NULL
  AND ct.contract_code LIKE 'CTR%'
  AND cmc.fp_id <> c.customer_fp_id;

DELETE FROM all_branch_contract_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM branch_contract_monthly_closing
WHERE closing_month = '2026-06';

DELETE FROM fp_monthly_performance_closing
WHERE closing_month = '2026-06';

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
