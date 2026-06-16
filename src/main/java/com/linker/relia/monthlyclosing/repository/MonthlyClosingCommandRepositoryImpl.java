package com.linker.relia.monthlyclosing.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class MonthlyClosingCommandRepositoryImpl implements MonthlyClosingCommandRepository {
    private final EntityManager entityManager;

    @Override
    public boolean existsClosingData(String closingMonth) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT
                    COALESCE((SELECT COUNT(*) FROM hr_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM organization_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM fp_commission_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM branch_commission_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM income_commission_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM branch_income_commission_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM branch_customer_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM all_branch_customer_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM branch_handover_monthly_closing WHERE closing_month = ?), 0)
                  + COALESCE((SELECT COUNT(*) FROM all_branch_handover_monthly_closing WHERE closing_month = ?), 0)
                """)
                .setParameter(1, closingMonth)
                .setParameter(2, closingMonth)
                .setParameter(3, closingMonth)
                .setParameter(4, closingMonth)
                .setParameter(5, closingMonth)
                .setParameter(6, closingMonth)
                .setParameter(7, closingMonth)
                .setParameter(8, closingMonth)
                .setParameter(9, closingMonth)
                .setParameter(10, closingMonth)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public void deleteExistingClosingData(String closingMonth) {
        executeUpdate("DELETE FROM all_branch_handover_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM branch_handover_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM all_branch_customer_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM branch_customer_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM branch_income_commission_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM income_commission_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM branch_commission_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM fp_commission_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM organization_monthly_closing WHERE closing_month = ?", closingMonth);
        executeUpdate("DELETE FROM hr_monthly_closing WHERE closing_month = ?", closingMonth);
    }

    @Override
    public void insertHrMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO hr_monthly_closing (
                    id, closing_month, user_id, emp_code, user_name, user_role, user_status, organization_id, closed_at
                )
                SELECT
                    UUID(), ?, u.id, u.emp_code, u.user_name, u.user_role, u.user_status, u.organization_id, ?
                FROM users u
                WHERE u.deleted_at IS NULL
                """, closingMonth, toTimestamp(closedAt));
    }

    @Override
    public void insertOrganizationMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO organization_monthly_closing (
                    id, closing_month, organization_id, organization_code, organization_name, organization_type, organization_status, closed_at
                )
                SELECT
                    UUID(), ?, o.id, o.organization_code, o.organization_name, o.organization_type, o.organization_status, ?
                FROM organizations o
                WHERE o.deleted_at IS NULL
                """, closingMonth, toTimestamp(closedAt));
    }

    @Override
    public void insertFpCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO fp_commission_monthly_closing (
                    id, closing_month, fp_id, organization_id, total_initial_payment_amount, total_maintenance_payment_amount,
                    total_recovery_collection_amount, total_payment_amount, net_commission_amount, contract_count,
                    recovery_contract_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
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
                    ?
                FROM payment_commission_records pcr
                JOIN users fp
                    ON fp.id = pcr.fp_id
                   AND fp.deleted_at IS NULL
                JOIN organizations org
                    ON org.id = pcr.organization_id
                   AND org.deleted_at IS NULL
                WHERE pcr.commission_month = ?
                GROUP BY pcr.fp_id, pcr.organization_id
                """, closingMonth, toTimestamp(closedAt), closingMonth);
    }

    @Override
    public void insertBranchCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO branch_commission_monthly_closing (
                    id, closing_month, organization_id, total_initial_payment_amount, total_maintenance_payment_amount,
                    total_recovery_collection_amount, total_payment_amount, net_commission_amount, fp_count, contract_count,
                    recovery_contract_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
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
                    ?
                FROM payment_commission_records pcr
                JOIN organizations org
                    ON org.id = pcr.organization_id
                   AND org.deleted_at IS NULL
                WHERE pcr.commission_month = ?
                GROUP BY pcr.organization_id
                """, closingMonth, toTimestamp(closedAt), closingMonth);
    }

    @Override
    public void insertIncomeCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO income_commission_monthly_closing (
                    id, closing_month, net_income_commission_amount, total_initial_gross_commission_amount,
                    total_maintenance_gross_commission_amount, total_payment_commission_amount,
                    total_insurance_recovery_amount, total_fp_recovery_collection_amount, closed_at
                )
                SELECT
                    UUID(),
                    ?,
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
                    ?
                FROM (
                    SELECT
                        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'INITIAL' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_initial_gross_commission_amount,
                        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'MAINTENANCE' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_maintenance_gross_commission_amount,
                        ROUND(COALESCE(SUM(CASE WHEN gcr.commission_type = 'RECOVERY' THEN gcr.gross_commission_amount ELSE 0 END), 0), 2) AS total_insurance_recovery_amount
                    FROM gross_commission_records gcr
                    WHERE gcr.commission_month = ?
                ) gross_summary
                CROSS JOIN (
                    SELECT
                        ROUND(COALESCE(SUM(CASE WHEN pcr.commission_type IN ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') THEN pcr.commission_amount ELSE 0 END), 0), 2) AS total_payment_commission_amount,
                        ROUND(COALESCE(SUM(CASE WHEN pcr.commission_type = 'RECOVERY_COLLECTION' THEN pcr.commission_amount ELSE 0 END), 0), 2) AS total_fp_recovery_collection_amount
                    FROM payment_commission_records pcr
                    WHERE pcr.commission_month = ?
                ) payment_summary
                """, closingMonth, toTimestamp(closedAt), closingMonth, closingMonth);
    }

    @Override
    public void insertBranchIncomeCommissionMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO branch_income_commission_monthly_closing (
                    id, closing_month, organization_id, net_income_commission_amount, total_initial_gross_commission_amount,
                    total_maintenance_gross_commission_amount, total_gross_commission_amount, total_payment_commission_amount,
                    total_insurance_recovery_amount, total_fp_recovery_collection_amount, contract_count, fp_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
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
                    ?
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
                    JOIN contracts ct ON ct.id = gcr.contract_id
                    JOIN users fp
                        ON fp.id = ct.fp_id
                       AND fp.deleted_at IS NULL
                    JOIN organizations org
                        ON org.id = fp.organization_id
                       AND org.deleted_at IS NULL
                    WHERE gcr.commission_month = ?
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
                    WHERE pcr.commission_month = ?
                    GROUP BY pcr.organization_id
                ) payment_summary
                    ON payment_summary.organization_id = gross_summary.organization_id
                """, closingMonth, toTimestamp(closedAt), closingMonth, closingMonth);
    }

    @Override
    public void insertBranchCustomerMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO branch_customer_monthly_closing (
                    id, closing_month, organization_id, fp_count, customer_count, interest_customer_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
                    org.id,
                    COALESCE(fp_summary.fp_count, 0),
                    COALESCE(customer_summary.customer_count, 0),
                    COALESCE(customer_summary.interest_customer_count, 0),
                    ?
                FROM organizations org
                LEFT JOIN (
                    SELECT
                        u.organization_id,
                        COUNT(*) AS fp_count
                    FROM users u
                    WHERE u.deleted_at IS NULL
                      AND u.user_role = 'FP'
                    GROUP BY u.organization_id
                ) fp_summary
                    ON fp_summary.organization_id = org.id
                LEFT JOIN (
                    SELECT
                        fp.organization_id,
                        COUNT(*) AS customer_count,
                        SUM(CASE WHEN c.interest_yn = TRUE THEN 1 ELSE 0 END) AS interest_customer_count
                    FROM customers c
                    JOIN users fp
                        ON fp.id = c.customer_fp_id
                       AND fp.deleted_at IS NULL
                    WHERE c.deleted_at IS NULL
                    GROUP BY fp.organization_id
                ) customer_summary
                    ON customer_summary.organization_id = org.id
                WHERE org.deleted_at IS NULL
                  AND org.organization_type = 'BRANCH'
                """, closingMonth, toTimestamp(closedAt));
    }

    @Override
    public void insertAllBranchCustomerMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO all_branch_customer_monthly_closing (
                    id, closing_month, fp_count, customer_count, interest_customer_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
                    COALESCE((SELECT COUNT(*) FROM users u WHERE u.deleted_at IS NULL AND u.user_role = 'FP'), 0),
                    COALESCE((SELECT COUNT(*) FROM customers c WHERE c.deleted_at IS NULL), 0),
                    COALESCE((SELECT COUNT(*) FROM customers c WHERE c.deleted_at IS NULL AND c.interest_yn = TRUE), 0),
                    ?
                """, closingMonth, toTimestamp(closedAt));
    }

    @Override
    public void insertBranchHandoverMonthlyClosing(String closingMonth, LocalDateTime closedAt, LocalDateTime monthStart, LocalDateTime nextMonthStart) {
        executeUpdate("""
                INSERT INTO branch_handover_monthly_closing (
                    id, closing_month, organization_id, requested_count, recommended_count,
                    manager_pending_count, approved_count, rejected_count, completed_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
                    org.id,
                    COALESCE(request_summary.requested_count, 0),
                    COALESCE(recommendation_summary.recommended_count, 0),
                    COALESCE(request_summary.manager_pending_count, 0),
                    COALESCE(recommendation_summary.approved_count, 0),
                    COALESCE(recommendation_summary.rejected_count, 0),
                    COALESCE(request_summary.completed_count, 0),
                    ?
                FROM organizations org
                LEFT JOIN (
                    SELECT
                        current_fp.organization_id,
                        COUNT(*) AS requested_count,
                        SUM(CASE WHEN hr.request_status = 'MANAGER_PENDING' THEN 1 ELSE 0 END) AS manager_pending_count,
                        SUM(CASE WHEN hr.request_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count
                    FROM handover_requests hr
                    JOIN users current_fp
                        ON current_fp.id = hr.current_fp_id
                       AND current_fp.deleted_at IS NULL
                    WHERE hr.deleted_at IS NULL
                      AND hr.current_fp_id IS NOT NULL
                      AND hr.created_at >= ?
                      AND hr.created_at < ?
                    GROUP BY current_fp.organization_id
                ) request_summary
                    ON request_summary.organization_id = org.id
                LEFT JOIN (
                    SELECT
                        current_fp.organization_id,
                        COUNT(*) AS recommended_count,
                        SUM(CASE WHEN rec.approval_status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
                        SUM(CASE WHEN rec.approval_status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
                    FROM handover_recommendations rec
                    JOIN handover_requests hr
                        ON hr.id = rec.handover_request_id
                       AND hr.deleted_at IS NULL
                    JOIN users current_fp
                        ON current_fp.id = hr.current_fp_id
                       AND current_fp.deleted_at IS NULL
                    WHERE hr.current_fp_id IS NOT NULL
                      AND hr.created_at >= ?
                      AND hr.created_at < ?
                    GROUP BY current_fp.organization_id
                ) recommendation_summary
                    ON recommendation_summary.organization_id = org.id
                WHERE org.deleted_at IS NULL
                  AND org.organization_type = 'BRANCH'
                """,
                closingMonth,
                toTimestamp(closedAt),
                toTimestamp(monthStart),
                toTimestamp(nextMonthStart),
                toTimestamp(monthStart),
                toTimestamp(nextMonthStart)
        );
    }

    @Override
    public void insertAllBranchHandoverMonthlyClosing(String closingMonth, LocalDateTime closedAt) {
        executeUpdate("""
                INSERT INTO all_branch_handover_monthly_closing (
                    id, closing_month, requested_count, recommended_count, manager_pending_count,
                    approved_count, rejected_count, completed_count, closed_at
                )
                SELECT
                    UUID(),
                    ?,
                    COALESCE(SUM(bhmc.requested_count), 0),
                    COALESCE(SUM(bhmc.recommended_count), 0),
                    COALESCE(SUM(bhmc.manager_pending_count), 0),
                    COALESCE(SUM(bhmc.approved_count), 0),
                    COALESCE(SUM(bhmc.rejected_count), 0),
                    COALESCE(SUM(bhmc.completed_count), 0),
                    ?
                FROM branch_handover_monthly_closing bhmc
                WHERE bhmc.closing_month = ?
                """, closingMonth, toTimestamp(closedAt), closingMonth);
    }

    private void executeUpdate(String sql, Object... params) {
        var query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.executeUpdate();
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value);
    }
}
