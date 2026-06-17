package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardContractStatusQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardContractStatusQueryRepositoryImpl implements DashboardContractStatusQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DashboardContractStatusQueryResult summarizeContractStatuses(UUID fpId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    count(*) as total_contract_count,
                    coalesce(sum(case when cmc.contract_status = 'MAINTENANCE' then 1 else 0 end), 0) as maintenance_contract_count,
                    coalesce(sum(case when cmc.contract_status = 'LAPSED' then 1 else 0 end), 0) as lapsed_contract_count,
                    coalesce(sum(case when cmc.contract_status = 'TERMINATED' then 1 else 0 end), 0) as terminated_contract_count,
                    coalesce(sum(case when cmc.contract_status = 'COMPLETED' then 1 else 0 end), 0) as completed_contract_count
                from contract_monthly_closing cmc
                where cmc.fp_id = :fpId
                  and cmc.closing_month = :closingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);

        Object[] row = (Object[]) query.getSingleResult();
        return new DashboardContractStatusQueryResult(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toLong(row[4])
        );
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
