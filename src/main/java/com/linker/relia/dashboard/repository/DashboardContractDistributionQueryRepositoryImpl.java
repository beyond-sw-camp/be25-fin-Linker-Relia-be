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

    private static final String BASE_FROM_CLAUSE = """
            from contract_monthly_closing cmc
            join contracts ct on ct.id = cmc.contract_id
            join users fp on fp.id = cmc.fp_id
            join insurance_products ip on ip.id = ct.insurance_product_id
            """;

    private static final String BASE_WHERE_CLAUSE = """
            where cmc.closing_month = :closingMonth
              and ct.deleted_at is null
              and fp.deleted_at is null
              and ip.deleted_at is null
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeAllInsuranceCompanyContractCounts(String closingMonth) {
        return summarizeInsuranceCompanyContractCounts(closingMonth, null, null);
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCompanyContractCountsByOrganization(
            UUID organizationId,
            String closingMonth
    ) {
        return summarizeInsuranceCompanyContractCounts(closingMonth, organizationId, null);
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCompanyContractCounts(
            UUID fpId,
            String closingMonth
    ) {
        return summarizeInsuranceCompanyContractCounts(closingMonth, null, fpId);
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeAllInsuranceCategoryContractCounts(String closingMonth) {
        return summarizeInsuranceCategoryContractCounts(closingMonth, null, null);
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCategoryContractCountsByOrganization(
            UUID organizationId,
            String closingMonth
    ) {
        return summarizeInsuranceCategoryContractCounts(closingMonth, organizationId, null);
    }

    @Override
    public List<DashboardContractDistributionQueryResult> summarizeInsuranceCategoryContractCounts(
            UUID fpId,
            String closingMonth
    ) {
        return summarizeInsuranceCategoryContractCounts(closingMonth, null, fpId);
    }

    private List<DashboardContractDistributionQueryResult> summarizeInsuranceCompanyContractCounts(
            String closingMonth,
            UUID organizationId,
            UUID fpId
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    ic.id as insurance_company_id,
                    ic.insurance_company_name,
                    count(*) as contract_count
                """ + BASE_FROM_CLAUSE + """
                join insurance_companies ic on ic.id = ip.insurance_company_id
                """ + BASE_WHERE_CLAUSE + """
                  and ic.deleted_at is null
                """ + organizationFilterClause(organizationId) + fpFilterClause(fpId) + """
                group by ic.id, ic.insurance_company_name
                order by contract_count desc, ic.insurance_company_name asc
                """);
        bindParameters(query, closingMonth, organizationId, fpId);
        return getQueryResults(query);
    }

    private List<DashboardContractDistributionQueryResult> summarizeInsuranceCategoryContractCounts(
            String closingMonth,
            UUID organizationId,
            UUID fpId
    ) {
        Query query = entityManager.createNativeQuery("""
                select
                    icat.id as insurance_category_id,
                    icat.insurance_category_name,
                    count(*) as contract_count
                """ + BASE_FROM_CLAUSE + """
                join insurance_categories icat on icat.id = ip.insurance_category_id
                """ + BASE_WHERE_CLAUSE + """
                  and icat.deleted_at is null
                """ + organizationFilterClause(organizationId) + fpFilterClause(fpId) + """
                group by icat.id, icat.insurance_category_name
                order by contract_count desc, icat.insurance_category_name asc
                """);
        bindParameters(query, closingMonth, organizationId, fpId);
        return getQueryResults(query);
    }

    private void bindParameters(Query query, String closingMonth, UUID organizationId, UUID fpId) {
        query.setParameter("closingMonth", closingMonth);
        if (organizationId != null) {
            query.setParameter("organizationId", organizationId.toString());
        }
        if (fpId != null) {
            query.setParameter("fpId", fpId.toString());
        }
    }

    private String organizationFilterClause(UUID organizationId) {
        return organizationId == null ? "" : "\n  and fp.organization_id = :organizationId\n";
    }

    private String fpFilterClause(UUID fpId) {
        return fpId == null ? "" : "\n  and cmc.fp_id = :fpId\n";
    }

    private List<DashboardContractDistributionQueryResult> getQueryResults(Query query) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toQueryResult)
                .toList();
    }

    private DashboardContractDistributionQueryResult toQueryResult(Object[] row) {
        return new DashboardContractDistributionQueryResult(
                toUuid(row[0]),
                (String) row[1],
                toLong(row[2])
        );
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(Objects.requireNonNull(value).toString());
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
