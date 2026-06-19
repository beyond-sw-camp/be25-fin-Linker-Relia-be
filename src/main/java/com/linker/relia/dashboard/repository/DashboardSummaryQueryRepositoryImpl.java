package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardSummaryQueryRepositoryImpl implements DashboardSummaryQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DashboardSummaryQueryResult findFpSummary(UUID fpId,
                                                     String closingMonth,
                                                     String comparisonClosingMonth) {
        return new DashboardSummaryQueryResult(
                findClosedNewContractCount(fpId, closingMonth),
                findClosedNewContractCount(fpId, comparisonClosingMonth),
                findClosedRetentionRate(fpId, closingMonth),
                findClosedRetentionRate(fpId, comparisonClosingMonth),
                findClosedBranchRank(fpId, closingMonth),
                findClosedBranchRank(fpId, comparisonClosingMonth),
                findClosedCustomerCount(fpId, closingMonth),
                findClosedCustomerCount(fpId, comparisonClosingMonth),
                findClosedNewHandoverCount(fpId, closingMonth),
                findClosedNewHandoverCount(fpId, comparisonClosingMonth),
                findClosedCommissionAmount(fpId, closingMonth),
                findClosedCommissionAmount(fpId, comparisonClosingMonth)
        );
    }

    private long findClosedNewContractCount(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(fmpc.new_contract_count, 0)
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleLongOrZero(query);
    }

    private BigDecimal findClosedRetentionRate(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(fmpc.retention_rate, 0)
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleBigDecimalOrZero(query);
    }

    private Integer findClosedBranchRank(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select fmpc.branch_rank
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleIntegerOrNull(query);
    }

    private long findClosedCustomerCount(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(fmpc.customer_count, 0)
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleLongOrZero(query);
    }

    private long findClosedNewHandoverCount(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(fmpc.new_handover_customer_count, 0)
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleLongOrZero(query);
    }

    private BigDecimal findClosedCommissionAmount(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(fcmc.net_commission_amount, 0)
                from fp_monthly_performance_closing fmpc
                left join fp_commission_monthly_closing fcmc
                       on fcmc.fp_id = fmpc.fp_id
                      and fcmc.closing_month = fmpc.closing_month
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleBigDecimalOrZero(query);
    }

    private long getSingleLongOrZero(Query query) {
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.isEmpty() ? 0L : toLong(rows.getFirst());
    }

    private Integer getSingleIntegerOrNull(Query query) {
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.isEmpty() ? null : toNullableInteger(rows.getFirst());
    }

    private BigDecimal getSingleBigDecimalOrZero(Query query) {
        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.isEmpty() ? BigDecimal.ZERO : toBigDecimal(rows.getFirst());
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private Integer toNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.intValue();
        }
        return ((Number) value).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
