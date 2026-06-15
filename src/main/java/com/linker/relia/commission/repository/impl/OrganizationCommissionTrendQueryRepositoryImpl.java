package com.linker.relia.commission.repository.impl;

import com.linker.relia.commission.dto.OrganizationCommissionMonthlyTrendQueryResult;
import com.linker.relia.commission.repository.custom.OrganizationCommissionTrendQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class OrganizationCommissionTrendQueryRepositoryImpl implements OrganizationCommissionTrendQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OrganizationCommissionMonthlyTrendQueryResult> findBranchTrendQueryResults(String startMonth,
                                                                                           String endMonth,
                                                                                           UUID organizationId) {
        Query query = entityManager.createNativeQuery("""
                select
                    'BRANCH' as scope,
                    bcmc.closing_month,
                    o.id as organization_id,
                    o.organization_name,
                    bcmc.total_initial_payment_amount,
                    bcmc.total_maintenance_payment_amount,
                    bcmc.total_recovery_collection_amount,
                    bcmc.total_payment_amount,
                    bcmc.net_commission_amount,
                    bcmc.fp_count,
                    bcmc.contract_count,
                    bcmc.recovery_contract_count
                from branch_commission_monthly_closing bcmc
                join organizations o on o.id = bcmc.organization_id
                where bcmc.organization_id = :organizationId
                  and bcmc.closing_month between :startMonth and :endMonth
                order by bcmc.closing_month asc
                """);
        query.setParameter("organizationId", organizationId.toString());
        query.setParameter("startMonth", startMonth);
        query.setParameter("endMonth", endMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    public List<OrganizationCommissionMonthlyTrendQueryResult> findHqTrendQueryResults(String startMonth,
                                                                                       String endMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    'HQ' as scope,
                    icmc.closing_month,
                    null as organization_id,
                    null as organization_name,
                    icmc.total_initial_gross_commission_amount,
                    icmc.total_maintenance_gross_commission_amount,
                    icmc.total_insurance_recovery_amount + icmc.total_fp_recovery_collection_amount as recovery_amount,
                    icmc.total_payment_commission_amount,
                    icmc.net_income_commission_amount,
                    coalesce(bc.fp_count, 0) as fp_count,
                    coalesce(bc.contract_count, 0) as contract_count,
                    coalesce(bc.recovery_contract_count, 0) as recovery_contract_count
                from income_commission_monthly_closing icmc
                left join (
                    select
                        closing_month,
                        sum(fp_count) as fp_count,
                        sum(contract_count) as contract_count,
                        sum(recovery_contract_count) as recovery_contract_count
                    from branch_commission_monthly_closing
                    group by closing_month
                ) bc on bc.closing_month = icmc.closing_month
                where icmc.closing_month between :startMonth and :endMonth
                order by icmc.closing_month asc
                """);
        query.setParameter("startMonth", startMonth);
        query.setParameter("endMonth", endMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toRow)
                .toList();
    }

    private OrganizationCommissionMonthlyTrendQueryResult toRow(Object[] row) {
        return new OrganizationCommissionMonthlyTrendQueryResult(
                (String) row[0],
                (String) row[1],
                toUuid(row[2]),
                (String) row[3],
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toBigDecimal(row[7]),
                toBigDecimal(row[8]),
                toLongWrapper(row[9]),
                toLongWrapper(row[10]),
                toLongWrapper(row[11])
        );
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }

    private Long toLongWrapper(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) value).longValue();
    }
}
