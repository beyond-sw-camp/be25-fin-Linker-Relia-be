package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardMonthlyCommissionTrendQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardMonthlyCommissionTrendQueryRepositoryImpl
        implements DashboardMonthlyCommissionTrendQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DashboardMonthlyCommissionTrendQueryResult> findMonthlyCommissionTrends(
            UUID fpId,
            String startMonth,
            String endMonth
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    fcmc.closing_month,
                    fcmc.net_commission_amount
                from fp_commission_monthly_closing fcmc
                where fcmc.fp_id = :fpId
                  and fcmc.closing_month between :startMonth and :endMonth
                order by fcmc.closing_month asc
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

    private DashboardMonthlyCommissionTrendQueryResult toQueryResult(Object[] row) {
        return new DashboardMonthlyCommissionTrendQueryResult(
                Objects.requireNonNull(row[0]).toString(),
                toBigDecimal(row[1])
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
