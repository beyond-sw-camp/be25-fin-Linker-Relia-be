-- Seed monthly commission closing tables from existing gross/payment commission data.

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
    CONCAT('92000000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY fp_source.closing_month, fp_source.fp_id), 12, '0')) AS id,
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
    CONCAT('92100000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY branch_source.closing_month, branch_source.organization_id), 12, '0')) AS id,
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
    GROUP BY
        fp_closing.closing_month,
        fp_closing.organization_id
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
    CONCAT('92200000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY income_source.closing_month), 12, '0')) AS id,
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
    CONCAT('92300000-0000-0000-0000-', LPAD(ROW_NUMBER() OVER (ORDER BY branch_income_source.closing_month, branch_income_source.organization_id), 12, '0')) AS id,
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
            ROUND(SUM(gcr.gross_commission_amount), 2) AS total_gross_commission_amount,
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
        GROUP BY
            gcr.commission_month,
            fp.organization_id
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
        GROUP BY
            pcr.commission_month,
            pcr.organization_id
    ) payment_summary
        ON payment_summary.closing_month = gross_summary.closing_month
       AND payment_summary.organization_id = gross_summary.organization_id
) branch_income_source;
