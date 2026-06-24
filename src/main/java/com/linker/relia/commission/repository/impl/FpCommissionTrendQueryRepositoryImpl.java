package com.linker.relia.commission.repository.impl;

import com.linker.relia.commission.dto.FpCommissionMonthlyTrendQueryResult;
import com.linker.relia.commission.repository.custom.FpCommissionTrendQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class FpCommissionTrendQueryRepositoryImpl implements FpCommissionTrendQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<FpCommissionMonthlyTrendQueryResult> findFpTrendQueryResults(String startMonth,
                                                                             String endMonth,
                                                                             UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select
                    fcmc.closing_month,
                    fcmc.total_initial_payment_amount,
                    fcmc.total_maintenance_payment_amount,
                    fcmc.total_recovery_collection_amount,
                    fcmc.total_payment_amount,
                    fcmc.net_commission_amount,
                    fcmc.contract_count,
                    fcmc.recovery_contract_count
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
                .map(this::toRow)
                .toList();
    }

    private FpCommissionMonthlyTrendQueryResult toRow(Object[] row) {
        return new FpCommissionMonthlyTrendQueryResult(
                (String) row[0],
                toBigDecimal(row[1]),
                toBigDecimal(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toLong(row[6]),
                toLong(row[7])
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
