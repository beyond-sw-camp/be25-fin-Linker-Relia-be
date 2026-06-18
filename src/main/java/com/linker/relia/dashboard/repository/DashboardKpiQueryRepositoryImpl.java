package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardKpiQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardKpiQueryRepositoryImpl implements DashboardKpiQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DashboardKpiQueryResult findHqKpi(String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    coalesce((
                        select abcmc.fp_count
                        from all_branch_customer_monthly_closing abcmc
                        where abcmc.closing_month = :closingMonth
                    ), 0) as fp_count,
                    coalesce((
                        select abcmc.customer_count
                        from all_branch_customer_monthly_closing abcmc
                        where abcmc.closing_month = :closingMonth
                    ), 0) as customer_count,
                    coalesce((
                        select abcmc.interest_customer_count
                        from all_branch_customer_monthly_closing abcmc
                        where abcmc.closing_month = :closingMonth
                    ), 0) as interest_customer_count,
                    coalesce((
                        select abctmc.total_contract_count
                        from all_branch_contract_monthly_closing abctmc
                        where abctmc.closing_month = :closingMonth
                          and abctmc.insurance_company_id is null
                          and abctmc.insurance_category_id is null
                    ), 0) as total_contract_count,
                    coalesce((
                        select abctmc.contract_success_rate
                        from all_branch_contract_monthly_closing abctmc
                        where abctmc.closing_month = :closingMonth
                          and abctmc.insurance_company_id is null
                          and abctmc.insurance_category_id is null
                    ), 0) as contract_success_rate,
                    coalesce((
                        select abctmc.retention_rate
                        from all_branch_contract_monthly_closing abctmc
                        where abctmc.closing_month = :closingMonth
                          and abctmc.insurance_company_id is null
                          and abctmc.insurance_category_id is null
                    ), 0) as retention_rate,
                    coalesce((
                        select abctmc.terminated_contract_count
                        from all_branch_contract_monthly_closing abctmc
                        where abctmc.closing_month = :closingMonth
                          and abctmc.insurance_company_id is null
                          and abctmc.insurance_category_id is null
                    ), 0) as terminated_contract_count,
                    coalesce((
                        select icmc.net_income_commission_amount
                        from income_commission_monthly_closing icmc
                        where icmc.closing_month = :closingMonth
                    ), 0) as net_income_commission_amount,
                    coalesce((
                        select icmc.total_payment_commission_amount
                        from income_commission_monthly_closing icmc
                        where icmc.closing_month = :closingMonth
                    ), 0) as total_payment_commission_amount
                """);
        query.setParameter("closingMonth", closingMonth);
        return getSingleResultOrZero(query);
    }

    @Override
    public DashboardKpiQueryResult findBranchKpi(UUID organizationId, String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    coalesce((
                        select bcmc.fp_count
                        from branch_customer_monthly_closing bcmc
                        where bcmc.organization_id = :organizationId
                          and bcmc.closing_month = :closingMonth
                    ), 0) as fp_count,
                    coalesce((
                        select bcmc.customer_count
                        from branch_customer_monthly_closing bcmc
                        where bcmc.organization_id = :organizationId
                          and bcmc.closing_month = :closingMonth
                    ), 0) as customer_count,
                    coalesce((
                        select bcmc.interest_customer_count
                        from branch_customer_monthly_closing bcmc
                        where bcmc.organization_id = :organizationId
                          and bcmc.closing_month = :closingMonth
                    ), 0) as interest_customer_count,
                    coalesce((
                        select bctmc.total_contract_count
                        from branch_contract_monthly_closing bctmc
                        where bctmc.organization_id = :organizationId
                          and bctmc.closing_month = :closingMonth
                          and bctmc.insurance_company_id is null
                          and bctmc.insurance_category_id is null
                    ), 0) as total_contract_count,
                    coalesce((
                        select bctmc.contract_success_rate
                        from branch_contract_monthly_closing bctmc
                        where bctmc.organization_id = :organizationId
                          and bctmc.closing_month = :closingMonth
                          and bctmc.insurance_company_id is null
                          and bctmc.insurance_category_id is null
                    ), 0) as contract_success_rate,
                    coalesce((
                        select bctmc.retention_rate
                        from branch_contract_monthly_closing bctmc
                        where bctmc.organization_id = :organizationId
                          and bctmc.closing_month = :closingMonth
                          and bctmc.insurance_company_id is null
                          and bctmc.insurance_category_id is null
                    ), 0) as retention_rate,
                    coalesce((
                        select bctmc.terminated_contract_count
                        from branch_contract_monthly_closing bctmc
                        where bctmc.organization_id = :organizationId
                          and bctmc.closing_month = :closingMonth
                          and bctmc.insurance_company_id is null
                          and bctmc.insurance_category_id is null
                    ), 0) as terminated_contract_count,
                    coalesce((
                        select bicmc.net_income_commission_amount
                        from branch_income_commission_monthly_closing bicmc
                        where bicmc.organization_id = :organizationId
                          and bicmc.closing_month = :closingMonth
                    ), 0) as net_income_commission_amount,
                    coalesce((
                        select bicmc.total_payment_commission_amount
                        from branch_income_commission_monthly_closing bicmc
                        where bicmc.organization_id = :organizationId
                          and bicmc.closing_month = :closingMonth
                    ), 0) as total_payment_commission_amount
                """);
        query.setParameter("organizationId", organizationId.toString());
        query.setParameter("closingMonth", closingMonth);
        return getSingleResultOrZero(query);
    }

    private DashboardKpiQueryResult getSingleResultOrZero(Query query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return new DashboardKpiQueryResult(
                    0L,
                    0L,
                    0L,
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
        return toQueryResult(rows.getFirst());
    }

    private DashboardKpiQueryResult toQueryResult(Object[] row) {
        return new DashboardKpiQueryResult(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toLong(row[6]),
                toBigDecimal(row[7]),
                toBigDecimal(row[8])
        );
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
