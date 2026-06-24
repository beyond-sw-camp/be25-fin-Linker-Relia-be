SET @SYSTEM_USER_ID = '30000000-0000-0000-0000-000000000001';

DELETE FROM customer_fp_history
WHERE id LIKE '89200000-0000-0000-0000-%';

DELETE FROM handover_recommendations
WHERE id LIKE '89100000-0000-0000-0000-%';

DELETE FROM handover_requests
WHERE id LIKE '89000000-0000-0000-0000-%';

DROP TEMPORARY TABLE IF EXISTS tmp_v19_handover_targets;
CREATE TEMPORARY TABLE tmp_v19_handover_targets (
    seq_no INT NOT NULL,
    customer_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    before_fp_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    before_fp_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    after_fp_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    after_fp_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    request_created_at DATETIME NOT NULL,
    approved_at DATETIME NOT NULL,
    PRIMARY KEY (seq_no),
    UNIQUE KEY uk_tmp_v19_handover_targets_customer (customer_id)
);

INSERT INTO tmp_v19_handover_targets (
    seq_no,
    customer_id,
    before_fp_id,
    before_fp_name,
    after_fp_id,
    after_fp_name,
    request_created_at,
    approved_at
)
SELECT
    ranked.seq_no,
    ranked.customer_id,
    ranked.before_fp_id,
    ranked.before_fp_name,
    ranked.after_fp_id,
    ranked.after_fp_name,
    CASE
        WHEN ranked.seq_no <= 6 THEN DATE_ADD('2025-12-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 12 THEN DATE_ADD('2026-01-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 18 THEN DATE_ADD('2026-02-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 24 THEN DATE_ADD('2026-03-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 30 THEN DATE_ADD('2026-04-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        ELSE DATE_ADD('2026-05-06 10:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
    END AS request_created_at,
    CASE
        WHEN ranked.seq_no <= 6 THEN DATE_ADD('2025-12-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 12 THEN DATE_ADD('2026-01-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 18 THEN DATE_ADD('2026-02-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 24 THEN DATE_ADD('2026-03-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        WHEN ranked.seq_no <= 30 THEN DATE_ADD('2026-04-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
        ELSE DATE_ADD('2026-05-12 15:00:00', INTERVAL MOD(ranked.seq_no - 1, 6) * 4 DAY)
    END AS approved_at
FROM (
    SELECT
        ROW_NUMBER() OVER (
            ORDER BY MOD(CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED) * 67, 257),
                     MOD(CAST(SUBSTRING_INDEX(c.customer_code, '-', -1) AS UNSIGNED) * 29, 131),
                     c.customer_code
        ) AS seq_no,
        c.id AS customer_id,
        candidate_fp.id AS before_fp_id,
        candidate_fp.user_name AS before_fp_name,
        current_fp.id AS after_fp_id,
        current_fp.user_name AS after_fp_name
    FROM customers c
    JOIN users current_fp
        ON current_fp.id = c.customer_fp_id
       AND current_fp.deleted_at IS NULL
       AND current_fp.user_role = 'FP'
    JOIN users candidate_fp
        ON candidate_fp.organization_id = current_fp.organization_id
       AND candidate_fp.deleted_at IS NULL
       AND candidate_fp.user_role = 'FP'
       AND candidate_fp.id <> current_fp.id
    WHERE c.deleted_at IS NULL
      AND c.customer_status = 'CONTRACTED'
      AND NOT EXISTS (
          SELECT 1
          FROM customer_fp_history cfh
          WHERE cfh.customer_id = c.id
      )
      AND candidate_fp.emp_code = (
          SELECT MIN(u2.emp_code)
          FROM users u2
          WHERE u2.organization_id = current_fp.organization_id
            AND u2.deleted_at IS NULL
            AND u2.user_role = 'FP'
            AND u2.id <> current_fp.id
      )
) ranked
WHERE ranked.seq_no <= 36;

INSERT INTO handover_requests (
    id,
    customer_id,
    current_fp_id,
    request_type,
    request_status,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    CONCAT('89000000-0000-0000-0000-', LPAD(seq_no, 12, '0')),
    customer_id,
    before_fp_id,
    CASE
        WHEN MOD(seq_no, 5) = 0 THEN 'RESIGNATION'
        ELSE 'VOLUNTARY'
    END,
    'COMPLETED',
    request_created_at,
    before_fp_id,
    approved_at,
    @SYSTEM_USER_ID
FROM tmp_v19_handover_targets;

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
    rejection_reason,
    created_at
)
SELECT
    CONCAT('89100000-0000-0000-0000-', LPAD(seq_no, 12, '0')),
    CONCAT('89000000-0000-0000-0000-', LPAD(seq_no, 12, '0')),
    after_fp_id,
    after_fp_name,
    'Demo handover recommendation for monthly reassignment',
    'APPROVED',
    @SYSTEM_USER_ID,
    approved_at,
    NULL,
    NULL,
    request_created_at
FROM tmp_v19_handover_targets;

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
    CONCAT('89200000-0000-0000-0000-', LPAD(target.seq_no, 12, '0')),
    COALESCE(existing_seq.max_sequence, 0) + 1,
    target.customer_id,
    CONCAT('89000000-0000-0000-0000-', LPAD(target.seq_no, 12, '0')),
    target.before_fp_id,
    target.before_fp_name,
    target.after_fp_id,
    target.after_fp_name,
    'Demo completed handover history backfill',
    target.approved_at,
    @SYSTEM_USER_ID
FROM tmp_v19_handover_targets target
LEFT JOIN (
    SELECT
        customer_id,
        MAX(customer_fp_sequence) AS max_sequence
    FROM customer_fp_history
    GROUP BY customer_id
) existing_seq
    ON existing_seq.customer_id = target.customer_id;

DELETE FROM all_branch_handover_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM branch_handover_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM fp_monthly_performance_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM all_branch_customer_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DELETE FROM branch_customer_monthly_closing
WHERE closing_month IN ('2025-12', '2026-01', '2026-02', '2026-03', '2026-04', '2026-05');

DROP TEMPORARY TABLE IF EXISTS tmp_v19_months;
CREATE TEMPORARY TABLE tmp_v19_months (
    closing_month VARCHAR(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    month_start DATETIME NOT NULL,
    next_month_start DATETIME NOT NULL,
    month_end DATE NOT NULL,
    closed_at DATETIME NOT NULL,
    PRIMARY KEY (closing_month)
);

INSERT INTO tmp_v19_months (
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

DROP TEMPORARY TABLE IF EXISTS tmp_v19_customer_status_snapshot;
CREATE TEMPORARY TABLE tmp_v19_customer_status_snapshot AS
SELECT
    m.closing_month,
    c.id AS customer_id,
    COALESCE(latest_status.after_status, 'PROSPECT') AS customer_status,
    COALESCE(latest_fp.after_fp_id, c.customer_fp_id) AS fp_id
FROM tmp_v19_months m
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
        FROM tmp_v19_months m
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
        FROM tmp_v19_months m
        JOIN customer_fp_history cfh
            ON cfh.changed_at <= m.closed_at
    ) ranked
    WHERE ranked.rn = 1
) latest_fp
    ON latest_fp.closing_month = m.closing_month
   AND latest_fp.customer_id = c.id;

DROP TEMPORARY TABLE IF EXISTS tmp_v19_customer_interest_snapshot;
CREATE TEMPORARY TABLE tmp_v19_customer_interest_snapshot AS
SELECT
    status_snapshot.closing_month,
    status_snapshot.customer_id,
    CASE
        WHEN status_snapshot.customer_status = 'CONTRACTED' AND interest_targets.interest_priority < 9 THEN TRUE
        ELSE FALSE
    END AS interest_yn
FROM tmp_v19_customer_status_snapshot status_snapshot
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
    FROM tmp_v19_months m
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
FROM tmp_v19_months m
JOIN organizations org
    ON org.deleted_at IS NULL
   AND org.organization_type = 'BRANCH'
LEFT JOIN (
    SELECT
        m.closing_month,
        u.organization_id,
        COUNT(*) AS fp_count
    FROM tmp_v19_months m
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
    FROM tmp_v19_customer_status_snapshot status_snapshot
    JOIN users fp
        ON fp.id = status_snapshot.fp_id
       AND fp.deleted_at IS NULL
    LEFT JOIN tmp_v19_customer_interest_snapshot interest_snapshot
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
FROM tmp_v19_months m
LEFT JOIN (
    SELECT
        m.closing_month,
        COUNT(*) AS fp_count
    FROM tmp_v19_months m
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
    FROM tmp_v19_customer_status_snapshot status_snapshot
    LEFT JOIN tmp_v19_customer_interest_snapshot interest_snapshot
        ON interest_snapshot.closing_month = status_snapshot.closing_month
       AND interest_snapshot.customer_id = status_snapshot.customer_id
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
FROM tmp_v19_months m
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
    FROM tmp_v19_months m
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
    FROM tmp_v19_months m
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
FROM tmp_v19_months m
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
        FROM tmp_v19_months m
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
            FROM tmp_v19_months m
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
            FROM tmp_v19_months m
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
            FROM tmp_v19_customer_status_snapshot status_snapshot
            GROUP BY status_snapshot.closing_month, status_snapshot.fp_id
        ) customer_summary
            ON customer_summary.closing_month = m.closing_month
           AND customer_summary.fp_id = fp.id
        LEFT JOIN (
            SELECT
                m.closing_month,
                h.after_fp_id AS fp_id,
                COUNT(DISTINCT h.customer_id) AS new_handover_customer_count
            FROM tmp_v19_months m
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

DROP TEMPORARY TABLE IF EXISTS tmp_v19_customer_interest_snapshot;
DROP TEMPORARY TABLE IF EXISTS tmp_v19_customer_status_snapshot;
DROP TEMPORARY TABLE IF EXISTS tmp_v19_months;
DROP TEMPORARY TABLE IF EXISTS tmp_v19_handover_targets;
