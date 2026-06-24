DELETE FROM all_branch_handover_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM branch_handover_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM all_branch_contract_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM branch_contract_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM fp_monthly_performance_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM all_branch_customer_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM branch_customer_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM organization_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM hr_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DROP TEMPORARY TABLE IF EXISTS tmp_v18_months;
CREATE TEMPORARY TABLE tmp_v18_months (
    closing_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    month_start DATETIME NOT NULL,
    next_month_start DATETIME NOT NULL,
    month_end DATE NOT NULL,
    closed_at DATETIME NOT NULL,
    PRIMARY KEY (closing_month)
);

INSERT INTO tmp_v18_months (
    closing_month,
    month_start,
    next_month_start,
    month_end,
    closed_at
)
VALUES
    ('2025-12', '2025-12-01 00:00:00', '2026-01-01 00:00:00', '2025-12-31', '2025-12-31 23:59:59'),
    ('2026-01', '2026-01-01 00:00:00', '2026-02-01 00:00:00', '2026-01-31', '2026-01-31 23:59:59'),
    ('2026-02', '2026-02-01 00:00:00', '2026-03-01 00:00:00', '2026-02-28', '2026-02-28 23:59:59'),
    ('2026-03', '2026-03-01 00:00:00', '2026-04-01 00:00:00', '2026-03-31', '2026-03-31 23:59:59'),
    ('2026-04', '2026-04-01 00:00:00', '2026-05-01 00:00:00', '2026-04-30', '2026-04-30 23:59:59'),
    ('2026-05', '2026-05-01 00:00:00', '2026-06-01 00:00:00', '2026-05-31', '2026-05-31 23:59:59');

DROP TEMPORARY TABLE IF EXISTS tmp_v18_customer_status_snapshot;
CREATE TEMPORARY TABLE tmp_v18_customer_status_snapshot AS
SELECT
    m.closing_month,
    c.id AS customer_id,
    COALESCE(latest_status.after_status, 'PROSPECT') AS customer_status,
    COALESCE(latest_fp.after_fp_id, c.customer_fp_id) AS fp_id
FROM tmp_v18_months m
JOIN customers c
    ON c.deleted_at IS NULL
LEFT JOIN (
    SELECT
        ranked.closing_month,
        ranked.customer_id,
        ranked.after_status
    FROM (
        SELECT
            m.closing_month,
            csh.customer_id,
            csh.after_status,
            ROW_NUMBER() OVER (
                PARTITION BY m.closing_month, csh.customer_id
                ORDER BY csh.changed_at DESC, csh.customer_status_sequence DESC
            ) AS rn
        FROM tmp_v18_months m
        JOIN customer_status_history csh
            ON csh.changed_at <= m.closed_at
    ) ranked
    WHERE ranked.rn = 1
) latest_status
    ON latest_status.closing_month = m.closing_month
   AND latest_status.customer_id = c.id
LEFT JOIN (
    SELECT
        ranked.closing_month,
        ranked.customer_id,
        ranked.after_fp_id
    FROM (
        SELECT
            m.closing_month,
            cfh.customer_id,
            cfh.after_fp_id,
            ROW_NUMBER() OVER (
                PARTITION BY m.closing_month, cfh.customer_id
                ORDER BY cfh.changed_at DESC, cfh.customer_fp_sequence DESC
            ) AS rn
        FROM tmp_v18_months m
        JOIN customer_fp_history cfh
            ON cfh.changed_at <= m.closed_at
    ) ranked
    WHERE ranked.rn = 1
) latest_fp
    ON latest_fp.closing_month = m.closing_month
   AND latest_fp.customer_id = c.id;

DROP TEMPORARY TABLE IF EXISTS tmp_v18_customer_interest_snapshot;
CREATE TEMPORARY TABLE tmp_v18_customer_interest_snapshot AS
SELECT
    status_snapshot.closing_month,
    status_snapshot.customer_id,
    CASE
        WHEN status_snapshot.customer_status = 'CONTRACTED' AND interest_targets.interest_priority < 9 THEN TRUE
        ELSE FALSE
    END AS interest_yn
FROM tmp_v18_customer_status_snapshot status_snapshot
LEFT JOIN (
    SELECT
        m.closing_month,
        ct.customer_id,
        MIN(
            CASE
                WHEN cmc.payment_status = 'UNPAID' THEN 1
                WHEN ip.is_renewable = TRUE
                     AND cmc.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN m.month_end AND DATE_ADD(m.month_end, INTERVAL 30 DAY) THEN 2
                WHEN ip.is_renewable = FALSE
                     AND cmc.contract_status = 'MAINTENANCE'
                     AND ct.contract_end_date BETWEEN m.month_end AND DATE_ADD(m.month_end, INTERVAL 30 DAY) THEN 3
                ELSE 9
            END
        ) AS interest_priority
    FROM tmp_v18_months m
    JOIN contract_monthly_closing cmc
        ON cmc.closing_month = m.closing_month
    JOIN contracts ct
        ON ct.id = cmc.contract_id
       AND ct.deleted_at IS NULL
    JOIN insurance_products ip
        ON ip.id = ct.insurance_product_id
       AND ip.deleted_at IS NULL
    GROUP BY m.closing_month, ct.customer_id
) interest_targets
    ON interest_targets.closing_month = status_snapshot.closing_month
   AND interest_targets.customer_id = status_snapshot.customer_id;

INSERT INTO hr_monthly_closing (
    id,
    closing_month,
    user_id,
    emp_code,
    user_name,
    user_role,
    user_status,
    organization_id,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    u.id,
    u.emp_code,
    u.user_name,
    u.user_role,
    u.user_status,
    u.organization_id,
    m.closed_at
FROM tmp_v18_months m
JOIN users u
    ON u.deleted_at IS NULL;

INSERT INTO organization_monthly_closing (
    id,
    closing_month,
    organization_id,
    organization_code,
    organization_name,
    organization_type,
    organization_status,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    o.id,
    o.organization_code,
    o.organization_name,
    o.organization_type,
    o.organization_status,
    m.closed_at
FROM tmp_v18_months m
JOIN organizations o
    ON o.deleted_at IS NULL;

INSERT INTO branch_customer_monthly_closing (
    id,
    closing_month,
    organization_id,
    fp_count,
    customer_count,
    interest_customer_count,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    org.id,
    COALESCE(fp_summary.fp_count, 0),
    COALESCE(customer_summary.customer_count, 0),
    COALESCE(customer_summary.interest_customer_count, 0),
    m.closed_at
FROM tmp_v18_months m
JOIN organizations org
    ON org.deleted_at IS NULL
   AND org.organization_type = 'BRANCH'
LEFT JOIN (
    SELECT
        m.closing_month,
        u.organization_id,
        COUNT(*) AS fp_count
    FROM tmp_v18_months m
    JOIN users u
        ON u.deleted_at IS NULL
       AND u.user_role = 'FP'
    GROUP BY m.closing_month, u.organization_id
) fp_summary
    ON fp_summary.closing_month = m.closing_month
   AND fp_summary.organization_id = org.id
LEFT JOIN (
    SELECT
        status_snapshot.closing_month,
        fp.organization_id,
        COUNT(*) AS customer_count,
        SUM(CASE WHEN interest_snapshot.interest_yn = TRUE THEN 1 ELSE 0 END) AS interest_customer_count
    FROM tmp_v18_customer_status_snapshot status_snapshot
    JOIN users fp
        ON fp.id = status_snapshot.fp_id
       AND fp.deleted_at IS NULL
    LEFT JOIN tmp_v18_customer_interest_snapshot interest_snapshot
        ON interest_snapshot.closing_month = status_snapshot.closing_month
       AND interest_snapshot.customer_id = status_snapshot.customer_id
    GROUP BY status_snapshot.closing_month, fp.organization_id
) customer_summary
    ON customer_summary.closing_month = m.closing_month
   AND customer_summary.organization_id = org.id;

INSERT INTO all_branch_customer_monthly_closing (
    id,
    closing_month,
    fp_count,
    customer_count,
    interest_customer_count,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    COALESCE(fp_summary.fp_count, 0),
    COALESCE(customer_summary.customer_count, 0),
    COALESCE(customer_summary.interest_customer_count, 0),
    m.closed_at
FROM tmp_v18_months m
LEFT JOIN (
    SELECT
        m.closing_month,
        COUNT(*) AS fp_count
    FROM tmp_v18_months m
    JOIN users u
        ON u.deleted_at IS NULL
       AND u.user_role = 'FP'
    GROUP BY m.closing_month
) fp_summary
    ON fp_summary.closing_month = m.closing_month
LEFT JOIN (
    SELECT
        status_snapshot.closing_month,
        COUNT(*) AS customer_count,
        SUM(CASE WHEN interest_snapshot.interest_yn = TRUE THEN 1 ELSE 0 END) AS interest_customer_count
    FROM tmp_v18_customer_status_snapshot status_snapshot
    LEFT JOIN tmp_v18_customer_interest_snapshot interest_snapshot
        ON interest_snapshot.closing_month = status_snapshot.closing_month
       AND interest_snapshot.customer_id = status_snapshot.customer_id
    GROUP BY status_snapshot.closing_month
) customer_summary
    ON customer_summary.closing_month = m.closing_month;

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
    m.closing_month,
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
    m.closed_at
FROM tmp_v18_months m
JOIN organizations org
    ON org.deleted_at IS NULL
   AND org.organization_type = 'BRANCH'
LEFT JOIN (
    SELECT
        cmc.closing_month,
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
    GROUP BY cmc.closing_month, fp.organization_id
) contract_summary
    ON contract_summary.closing_month = m.closing_month
   AND contract_summary.organization_id = org.id
LEFT JOIN (
    SELECT
        status_snapshot.closing_month,
        fp.organization_id,
        SUM(CASE WHEN status_snapshot.customer_status = 'PROSPECT' THEN 1 ELSE 0 END) AS prospect_customer_count,
        SUM(CASE WHEN status_snapshot.customer_status = 'CONTRACTED' THEN 1 ELSE 0 END) AS contracted_customer_count
    FROM tmp_v18_customer_status_snapshot status_snapshot
    JOIN users fp
        ON fp.id = status_snapshot.fp_id
       AND fp.deleted_at IS NULL
    GROUP BY status_snapshot.closing_month, fp.organization_id
) customer_summary
    ON customer_summary.closing_month = m.closing_month
   AND customer_summary.organization_id = org.id;

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
    m.closing_month,
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
    m.closed_at
FROM tmp_v18_months m
LEFT JOIN (
    SELECT
        cmc.closing_month,
        COUNT(*) AS total_contract_count,
        SUM(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END) AS active_contract_count,
        SUM(CASE WHEN cmc.contract_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_contract_count,
        SUM(CASE WHEN cmc.contract_status IN ('LAPSED', 'TERMINATED') THEN 1 ELSE 0 END) AS terminated_contract_count,
        ROUND(COALESCE(SUM(cmc.monthly_premium), 0), 2) AS total_monthly_premium_amount
    FROM contract_monthly_closing cmc
    GROUP BY cmc.closing_month
) contract_summary
    ON contract_summary.closing_month = m.closing_month
LEFT JOIN (
    SELECT
        status_snapshot.closing_month,
        SUM(CASE WHEN status_snapshot.customer_status = 'PROSPECT' THEN 1 ELSE 0 END) AS prospect_customer_count,
        SUM(CASE WHEN status_snapshot.customer_status = 'CONTRACTED' THEN 1 ELSE 0 END) AS contracted_customer_count
    FROM tmp_v18_customer_status_snapshot status_snapshot
    GROUP BY status_snapshot.closing_month
) customer_summary
    ON customer_summary.closing_month = m.closing_month;

INSERT INTO branch_handover_monthly_closing (
    id,
    closing_month,
    organization_id,
    requested_count,
    recommended_count,
    manager_pending_count,
    approved_count,
    rejected_count,
    completed_count,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    org.id,
    COALESCE(request_summary.requested_count, 0),
    COALESCE(recommendation_summary.recommended_count, 0),
    COALESCE(request_summary.manager_pending_count, 0),
    COALESCE(recommendation_summary.approved_count, 0),
    COALESCE(recommendation_summary.rejected_count, 0),
    COALESCE(request_summary.completed_count, 0),
    m.closed_at
FROM tmp_v18_months m
JOIN organizations org
    ON org.deleted_at IS NULL
   AND org.organization_type = 'BRANCH'
LEFT JOIN (
    SELECT
        m.closing_month,
        current_fp.organization_id,
        COUNT(*) AS requested_count,
        SUM(CASE WHEN hr.request_status = 'MANAGER_PENDING' THEN 1 ELSE 0 END) AS manager_pending_count,
        SUM(CASE WHEN hr.request_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count
    FROM tmp_v18_months m
    JOIN handover_requests hr
        ON hr.deleted_at IS NULL
       AND hr.current_fp_id IS NOT NULL
       AND hr.created_at >= m.month_start
       AND hr.created_at < m.next_month_start
    JOIN users current_fp
        ON current_fp.id = hr.current_fp_id
       AND current_fp.deleted_at IS NULL
    GROUP BY m.closing_month, current_fp.organization_id
) request_summary
    ON request_summary.closing_month = m.closing_month
   AND request_summary.organization_id = org.id
LEFT JOIN (
    SELECT
        m.closing_month,
        current_fp.organization_id,
        COUNT(*) AS recommended_count,
        SUM(CASE WHEN rec.approval_status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
        SUM(CASE WHEN rec.approval_status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
    FROM tmp_v18_months m
    JOIN handover_requests hr
        ON hr.deleted_at IS NULL
       AND hr.current_fp_id IS NOT NULL
       AND hr.created_at >= m.month_start
       AND hr.created_at < m.next_month_start
    JOIN handover_recommendations rec
        ON rec.handover_request_id = hr.id
    JOIN users current_fp
        ON current_fp.id = hr.current_fp_id
       AND current_fp.deleted_at IS NULL
    GROUP BY m.closing_month, current_fp.organization_id
) recommendation_summary
    ON recommendation_summary.closing_month = m.closing_month
   AND recommendation_summary.organization_id = org.id;

INSERT INTO all_branch_handover_monthly_closing (
    id,
    closing_month,
    requested_count,
    recommended_count,
    manager_pending_count,
    approved_count,
    rejected_count,
    completed_count,
    closed_at
)
SELECT
    UUID(),
    m.closing_month,
    COALESCE(SUM(bhmc.requested_count), 0),
    COALESCE(SUM(bhmc.recommended_count), 0),
    COALESCE(SUM(bhmc.manager_pending_count), 0),
    COALESCE(SUM(bhmc.approved_count), 0),
    COALESCE(SUM(bhmc.rejected_count), 0),
    COALESCE(SUM(bhmc.completed_count), 0),
    m.closed_at
FROM tmp_v18_months m
LEFT JOIN branch_handover_monthly_closing bhmc
    ON bhmc.closing_month = m.closing_month
GROUP BY m.closing_month, m.closed_at;

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
            PARTITION BY base.closing_month, base.organization_id
            ORDER BY base.performance_score DESC, base.commission_amount DESC,
                     base.completed_contract_count DESC, base.fp_id ASC
        ) AS branch_rank,
        ROW_NUMBER() OVER (
            PARTITION BY base.closing_month
            ORDER BY base.performance_score DESC, base.commission_amount DESC,
                     base.completed_contract_count DESC, base.fp_id ASC
        ) AS total_rank,
        base.closed_at
    FROM (
        SELECT
            m.closing_month,
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
            m.closed_at
        FROM tmp_v18_months m
        JOIN users fp
            ON fp.deleted_at IS NULL
           AND fp.user_role = 'FP'
        JOIN organizations org
            ON org.id = fp.organization_id
           AND org.deleted_at IS NULL
           AND org.organization_type = 'BRANCH'
        LEFT JOIN (
            SELECT
                m.closing_month,
                ct.fp_id,
                COUNT(*) AS new_contract_count
            FROM tmp_v18_months m
            JOIN contracts ct
                ON ct.deleted_at IS NULL
               AND ct.contract_date >= DATE(m.month_start)
               AND ct.contract_date < DATE(m.next_month_start)
            GROUP BY m.closing_month, ct.fp_id
        ) new_contract_summary
            ON new_contract_summary.closing_month = m.closing_month
           AND new_contract_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                cmc.closing_month,
                cmc.fp_id,
                COUNT(*) AS completed_contract_count,
                SUM(CASE WHEN cmc.contract_status = 'MAINTENANCE' THEN 1 ELSE 0 END) AS active_contract_count
            FROM contract_monthly_closing cmc
            GROUP BY cmc.closing_month, cmc.fp_id
        ) contract_summary
            ON contract_summary.closing_month = m.closing_month
           AND contract_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                m.closing_month,
                c.fp_id,
                COUNT(*) AS consultation_count
            FROM tmp_v18_months m
            JOIN consultations c
                ON c.deleted_at IS NULL
               AND c.consulted_at >= m.month_start
               AND c.consulted_at < m.next_month_start
            GROUP BY m.closing_month, c.fp_id
        ) consultation_summary
            ON consultation_summary.closing_month = m.closing_month
           AND consultation_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                status_snapshot.closing_month,
                status_snapshot.fp_id,
                COUNT(*) AS customer_count
            FROM tmp_v18_customer_status_snapshot status_snapshot
            GROUP BY status_snapshot.closing_month, status_snapshot.fp_id
        ) customer_summary
            ON customer_summary.closing_month = m.closing_month
           AND customer_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                m.closing_month,
                h.after_fp_id AS fp_id,
                COUNT(DISTINCT h.customer_id) AS new_handover_customer_count
            FROM tmp_v18_months m
            JOIN customer_fp_history h
                ON h.changed_at >= m.month_start
               AND h.changed_at < m.next_month_start
            GROUP BY m.closing_month, h.after_fp_id
        ) handover_summary
            ON handover_summary.closing_month = m.closing_month
           AND handover_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                fcmc.closing_month,
                fcmc.fp_id,
                fcmc.net_commission_amount AS commission_amount
            FROM fp_commission_monthly_closing fcmc
            WHERE fcmc.closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05')
        ) commission_summary
            ON commission_summary.closing_month = m.closing_month
           AND commission_summary.fp_id = fp.id
    ) base
) ranked;

DROP TEMPORARY TABLE IF EXISTS tmp_v18_customer_interest_snapshot;
DROP TEMPORARY TABLE IF EXISTS tmp_v18_customer_status_snapshot;
DROP TEMPORARY TABLE IF EXISTS tmp_v18_months;
