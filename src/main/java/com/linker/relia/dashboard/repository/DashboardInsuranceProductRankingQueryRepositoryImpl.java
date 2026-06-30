package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardInsuranceProductRankingItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardInsuranceProductRankingQueryRepositoryImpl
        implements DashboardInsuranceProductRankingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DashboardInsuranceProductRankingItemResponse> findProductRankings(
            String closingMonth,
            UUID organizationId,
            UUID fpId,
            int limit
    ) {
        if (fpId != null) {
            return findFpProductRankings(closingMonth, fpId, limit);
        }

        return findOrganizationProductRankings(closingMonth, organizationId, limit);
    }

    private List<DashboardInsuranceProductRankingItemResponse> findFpProductRankings(
            String closingMonth,
            UUID fpId,
            int limit
    ) {

        Query query = entityManager.createNativeQuery("""
                select
                    ranked.insurance_product_name,
                    ranked.insurance_company_name,
                    ranked.contract_count
                from (
                    select
                        dense_rank() over (
                            order by
                                product_summary.contract_count desc,
                                product_summary.insurance_product_name asc,
                                product_summary.insurance_product_id asc
                        ) as ranking,
                        product_summary.insurance_product_id,
                        product_summary.insurance_product_name,
                        product_summary.insurance_company_name,
                        product_summary.contract_count
                    from (
                        select
                            pcr.insurance_product_id,
                            ip.insurance_product_name,
                            ic.insurance_company_name,
                            count(distinct pcr.contract_id) as contract_count
                        from payment_commission_records pcr
                        join insurance_products ip on ip.id = pcr.insurance_product_id
                        join insurance_companies ic on ic.id = ip.insurance_company_id
                        where pcr.commission_month = :closingMonth
                          and pcr.fp_id = :fpId
                        group by
                            pcr.insurance_product_id,
                            ip.insurance_product_name,
                            ic.insurance_company_name
                    ) product_summary
                ) ranked
                order by ranked.ranking asc, ranked.insurance_product_name asc, ranked.insurance_product_id asc
                """);

        query.setParameter("closingMonth", closingMonth);
        query.setParameter("fpId", fpId.toString());
        query.setMaxResults(limit);

        return toResponses(query.getResultList());
    }

    private List<DashboardInsuranceProductRankingItemResponse> findOrganizationProductRankings(
            String closingMonth,
            UUID organizationId,
            int limit
    ) {
        String paymentOrganizationFilter = organizationId == null ? "" : "\n      and pcr.organization_id = :organizationId\n";
        String grossOrganizationFilter = organizationId == null ? "" : "\n      and fp.organization_id = :organizationId\n";

        Query query = entityManager.createNativeQuery("""
                with payment_companies as (
                    select distinct
                        pcr.insurance_company_id
                    from payment_commission_records pcr
                    where pcr.commission_month = :closingMonth
                """ + paymentOrganizationFilter + """
                ),
                product_summary as (
                    select
                        pcr.insurance_product_id,
                        ip.insurance_product_name,
                        ic.insurance_company_name,
                        count(distinct pcr.contract_id) as contract_count
                    from payment_commission_records pcr
                    join insurance_products ip on ip.id = pcr.insurance_product_id
                    join insurance_companies ic on ic.id = pcr.insurance_company_id
                    where pcr.commission_month = :closingMonth
                """ + paymentOrganizationFilter + """
                    group by
                        pcr.insurance_product_id,
                        ip.insurance_product_name,
                        ic.insurance_company_name

                    union all

                    select
                        gcr.insurance_product_id,
                        ip.insurance_product_name,
                        ic.insurance_company_name,
                        count(distinct gcr.contract_id) as contract_count
                    from gross_commission_records gcr
                    join contracts ct on ct.id = gcr.contract_id
                    join users fp on fp.id = ct.fp_id
                    join insurance_products ip on ip.id = gcr.insurance_product_id
                    join insurance_companies ic on ic.id = gcr.insurance_company_id
                    where gcr.commission_month = :closingMonth
                      and not exists (
                          select 1
                          from payment_companies pc
                          where pc.insurance_company_id = gcr.insurance_company_id
                      )
                """ + grossOrganizationFilter + """
                    group by
                        gcr.insurance_product_id,
                        ip.insurance_product_name,
                        ic.insurance_company_name
                )
                select
                    ranked.insurance_product_name,
                    ranked.insurance_company_name,
                    ranked.contract_count
                from (
                    select
                        dense_rank() over (
                            order by
                                product_summary.contract_count desc,
                                product_summary.insurance_product_name asc,
                                product_summary.insurance_product_id asc
                        ) as ranking,
                        product_summary.insurance_product_id,
                        product_summary.insurance_product_name,
                        product_summary.insurance_company_name,
                        product_summary.contract_count
                    from product_summary
                ) ranked
                order by ranked.ranking asc, ranked.insurance_product_name asc, ranked.insurance_product_id asc
                """);

        query.setParameter("closingMonth", closingMonth);
        if (organizationId != null) {
            query.setParameter("organizationId", organizationId.toString());
        }
        query.setMaxResults(limit);

        return toResponses(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<DashboardInsuranceProductRankingItemResponse> toResponses(List<?> rawRows) {
        List<Object[]> rows = (List<Object[]>) rawRows;
        return rows.stream()
                .map(this::toResponse)
                .toList();
    }

    private DashboardInsuranceProductRankingItemResponse toResponse(Object[] row) {
        return DashboardInsuranceProductRankingItemResponse.builder()
                .insuranceProductName((String) row[0])
                .insuranceCompanyName((String) row[1])
                .contractCount(toLong(row[2]))
                .build();
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
