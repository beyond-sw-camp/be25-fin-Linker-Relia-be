package com.linker.relia.commission.repository.impl;

import com.linker.relia.commission.dto.InsuranceCompanyCommissionSummaryQueryResult;
import com.linker.relia.commission.repository.custom.CommissionInsuranceCompanyQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class CommissionInsuranceCompanyQueryRepositoryImpl implements CommissionInsuranceCompanyQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<InsuranceCompanyCommissionSummaryQueryResult> findFpSummaries(String closingMonth, UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select
                    ic.id,
                    ic.insurance_company_name,
                    round(sum(case when pcr.commission_type = 'INITIAL_PAYMENT' then pcr.commission_amount else 0 end), 2) as total_initial_amount,
                    round(sum(case when pcr.commission_type = 'MAINTENANCE_PAYMENT' then pcr.commission_amount else 0 end), 2) as total_maintenance_amount,
                    round(sum(case when pcr.commission_type = 'RECOVERY_COLLECTION' then pcr.commission_amount else 0 end), 2) as total_recovery_amount,
                    round(sum(case when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') then pcr.commission_amount else 0 end), 2) as total_payment_amount,
                    round(
                        sum(case when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') then pcr.commission_amount else 0 end)
                        - sum(case when pcr.commission_type = 'RECOVERY_COLLECTION' then pcr.commission_amount else 0 end),
                        2
                    ) as net_amount,
                    count(distinct pcr.contract_id) as contract_count
                from payment_commission_records pcr
                join insurance_companies ic on ic.id = pcr.insurance_company_id
                where pcr.commission_month = :closingMonth
                  and pcr.fp_id = :fpId
                group by ic.id, ic.insurance_company_name
                order by total_payment_amount desc, ic.insurance_company_name asc
                """);
        query.setParameter("closingMonth", closingMonth);
        query.setParameter("fpId", fpId.toString());
        return toQueryResults(query.getResultList());
    }

    @Override
    public List<InsuranceCompanyCommissionSummaryQueryResult> findBranchSummaries(String closingMonth, UUID organizationId) {
        Query query = entityManager.createNativeQuery("""
                select
                    ic.id,
                    ic.insurance_company_name,
                    round(sum(case when pcr.commission_type = 'INITIAL_PAYMENT' then pcr.commission_amount else 0 end), 2) as total_initial_amount,
                    round(sum(case when pcr.commission_type = 'MAINTENANCE_PAYMENT' then pcr.commission_amount else 0 end), 2) as total_maintenance_amount,
                    round(sum(case when pcr.commission_type = 'RECOVERY_COLLECTION' then pcr.commission_amount else 0 end), 2) as total_recovery_amount,
                    round(sum(case when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') then pcr.commission_amount else 0 end), 2) as total_payment_amount,
                    round(
                        sum(case when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') then pcr.commission_amount else 0 end)
                        - sum(case when pcr.commission_type = 'RECOVERY_COLLECTION' then pcr.commission_amount else 0 end),
                        2
                    ) as net_amount,
                    count(distinct pcr.contract_id) as contract_count
                from payment_commission_records pcr
                join insurance_companies ic on ic.id = pcr.insurance_company_id
                where pcr.commission_month = :closingMonth
                  and pcr.organization_id = :organizationId
                group by ic.id, ic.insurance_company_name
                order by total_payment_amount desc, ic.insurance_company_name asc
                """);
        query.setParameter("closingMonth", closingMonth);
        query.setParameter("organizationId", organizationId.toString());
        return toQueryResults(query.getResultList());
    }

    @Override
    public List<InsuranceCompanyCommissionSummaryQueryResult> findHqSummaries(String closingMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    ic.id,
                    ic.insurance_company_name,
                    coalesce(gross_summary.total_initial_amount, 0) as total_initial_amount,
                    coalesce(gross_summary.total_maintenance_amount, 0) as total_maintenance_amount,
                    coalesce(gross_summary.total_recovery_amount, 0) as total_recovery_amount,
                    coalesce(payment_summary.total_payment_amount, 0) as total_payment_amount,
                    round(
                        coalesce(gross_summary.total_initial_amount, 0)
                        + coalesce(gross_summary.total_maintenance_amount, 0)
                        - coalesce(payment_summary.total_payment_amount, 0)
                        - coalesce(gross_summary.total_recovery_amount, 0)
                        + coalesce(payment_summary.total_fp_recovery_amount, 0),
                        2
                    ) as net_amount,
                    coalesce(payment_summary.contract_count, gross_summary.contract_count, 0) as contract_count
                from insurance_companies ic
                left join (
                    select
                        gcr.insurance_company_id,
                        round(sum(case when gcr.commission_type = 'INITIAL' then gcr.gross_commission_amount else 0 end), 2) as total_initial_amount,
                        round(sum(case when gcr.commission_type = 'MAINTENANCE' then gcr.gross_commission_amount else 0 end), 2) as total_maintenance_amount,
                        round(sum(case when gcr.commission_type = 'RECOVERY' then gcr.gross_commission_amount else 0 end), 2) as total_recovery_amount,
                        round(sum(gcr.gross_commission_amount), 2) as total_gross_amount,
                        count(distinct gcr.contract_id) as contract_count
                    from gross_commission_records gcr
                    where gcr.commission_month = :closingMonth
                    group by gcr.insurance_company_id
                ) gross_summary on gross_summary.insurance_company_id = ic.id
                left join (
                    select
                        pcr.insurance_company_id,
                        round(sum(case when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT') then pcr.commission_amount else 0 end), 2) as total_payment_amount,
                        round(sum(case when pcr.commission_type = 'RECOVERY_COLLECTION' then pcr.commission_amount else 0 end), 2) as total_fp_recovery_amount,
                        count(distinct pcr.contract_id) as contract_count
                    from payment_commission_records pcr
                    where pcr.commission_month = :closingMonth
                    group by pcr.insurance_company_id
                ) payment_summary on payment_summary.insurance_company_id = ic.id
                where gross_summary.insurance_company_id is not null
                   or payment_summary.insurance_company_id is not null
                order by total_payment_amount desc, ic.insurance_company_name asc
                """);
        query.setParameter("closingMonth", closingMonth);
        return toQueryResults(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<InsuranceCompanyCommissionSummaryQueryResult> toQueryResults(List<?> rawRows) {
        return ((List<Object[]>) rawRows).stream()
                .map(this::toQueryResult)
                .toList();
    }

    private InsuranceCompanyCommissionSummaryQueryResult toQueryResult(Object[] row) {
        return new InsuranceCompanyCommissionSummaryQueryResult(
                toUuid(row[0]),
                (String) row[1],
                toBigDecimal(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toLong(row[7])
        );
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(Objects.requireNonNull(value).toString());
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
