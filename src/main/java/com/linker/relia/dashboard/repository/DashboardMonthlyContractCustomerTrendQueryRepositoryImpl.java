package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardMonthlyContractCustomerTrendQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardMonthlyContractCustomerTrendQueryRepositoryImpl
        implements DashboardMonthlyContractCustomerTrendQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DashboardMonthlyContractCustomerTrendQueryResult> findMonthlyContractCustomerTrends(
            UUID fpId,
            String startMonth,
            String endMonth
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    fmpc.closing_month,
                    fmpc.new_contract_count,
                    fmpc.customer_count
                from fp_monthly_performance_closing fmpc
                where fmpc.fp_id = :fpId
                  and fmpc.closing_month between :startMonth and :endMonth
                order by fmpc.closing_month asc
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("startMonth", startMonth);
        query.setParameter("endMonth", endMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toQueryResult)
                .toList();
    }

    private DashboardMonthlyContractCustomerTrendQueryResult toQueryResult(Object[] row) {
        return new DashboardMonthlyContractCustomerTrendQueryResult(
                Objects.requireNonNull(row[0]).toString(),
                toLong(row[1]),
                toLong(row[2])
        );
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
