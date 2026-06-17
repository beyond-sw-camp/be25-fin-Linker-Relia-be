package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardContractDistributionQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardContractDistributionQueryRepositoryImpl
        implements DashboardContractDistributionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCompanyContractCounts(
            UUID fpId,
            String closingMonth
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    ic.id as insurance_company_id,
                    ic.insurance_company_name,
                    count(*) as contract_count
                from contract_monthly_closing cmc
                join contracts ct on ct.id = cmc.contract_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                where cmc.fp_id = :fpId
                  and cmc.closing_month = :closingMonth
                  and ct.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                group by ic.id, ic.insurance_company_name
                order by contract_count desc, ic.insurance_company_name asc
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toQueryResult)
                .toList();
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCategoryContractCounts(
            UUID fpId,
            String closingMonth
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    icat.id as insurance_category_id,
                    icat.insurance_category_name,
                    count(*) as contract_count
                from contract_monthly_closing cmc
                join contracts ct on ct.id = cmc.contract_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_categories icat on icat.id = ip.insurance_category_id
                where cmc.fp_id = :fpId
                  and cmc.closing_month = :closingMonth
                  and ct.deleted_at is null
                  and ip.deleted_at is null
                  and icat.deleted_at is null
                group by icat.id, icat.insurance_category_name
                order by contract_count desc, icat.insurance_category_name asc
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toQueryResult)
                .toList();
    }

    private DashboardContractDistributionQueryResult toQueryResult(Object[] row) {
        return new DashboardContractDistributionQueryResult(
                UUID.fromString((String) row[0]),
                (String) row[1],
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
