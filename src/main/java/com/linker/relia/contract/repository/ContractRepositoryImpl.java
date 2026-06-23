package com.linker.relia.contract.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.contract.dto.ContractDetailQueryResult;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListSort;
import com.linker.relia.contract.dto.ContractListStatus;
import com.linker.relia.contract.dto.ContractMonthlyTrendResponse;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContractRepositoryImpl implements ContractRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId) {
        String sql = """
                select
                    count(*) as total_contract_count,
                    coalesce(sum(ct.monthly_premium), 0) as total_monthly_premium,
                    coalesce(sum(case when ct.contract_status = 'MAINTENANCE' then 1 else 0 end), 0) as maintenance_count,
                    coalesce(sum(case when ct.contract_status = 'COMPLETED' then 1 else 0 end), 0) as completed_count,
                    coalesce(sum(case when ct.contract_status = 'TERMINATED' then 1 else 0 end), 0) as terminated_count,
                    coalesce(sum(case when ct.contract_status = 'LAPSED' then 1 else 0 end), 0) as lapsed_count
                from contracts ct
                where ct.customer_id = :customerId
                  and ct.deleted_at is null
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("customerId", customerId.toString());

        Object[] row = (Object[]) query.getSingleResult();
        Map<String, Long> contractStatusCounts = CustomerContractSummaryResponse.defaultContractStatusCounts();
        contractStatusCounts.put("MAINTENANCE", toLong(row[2]));
        contractStatusCounts.put("COMPLETED", toLong(row[3]));
        contractStatusCounts.put("TERMINATED", toLong(row[4]));
        contractStatusCounts.put("LAPSED", toLong(row[5]));

        return CustomerContractSummaryResponse.builder()
                .totalContractCount(toLong(row[0]))
                .totalMonthlyPremium(toBigDecimal(row[1]))
                .contractStatusCounts(contractStatusCounts)
                .build();
    }

    @Override
    public ContractSummaryResponse summarizeHoldingContracts(AccessScope accessScope,
                                                             String organizationCode,
                                                             UUID insuranceCompanyId,
                                                             LocalDate referenceDate,
                                                             LocalDate dueDateLimit) {
        String sql = """
                select
                    count(*) as total_contract_count,
                    coalesce(sum(case
                        when ct.contract_status = 'MAINTENANCE' then 1 else 0
                    end), 0) as normal_payment_count,
                    coalesce(sum(case
                        when ct.contract_status = 'LAPSED' then 1 else 0
                    end), 0) as unpaid_count,
                    coalesce(sum(case
                        when ct.contract_status = 'LAPSED' then 1 else 0
                    end), 0) as lapse_expected_count,
                    coalesce(sum(case
                        when ct.contract_status = 'MAINTENANCE'
                         and ip.is_renewable = false
                         and ct.contract_end_date between :referenceDate and :dueDateLimit then 1 else 0
                    end), 0) as expiring_soon_count,
                    coalesce(sum(case
                        when ct.contract_status = 'MAINTENANCE'
                         and ip.is_renewable = true
                         and ct.contract_end_date between :referenceDate and :dueDateLimit then 1 else 0
                    end), 0) as renewal_soon_count
                from contracts ct
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                where ct.contract_status in ('MAINTENANCE', 'LAPSED')
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:insuranceCompanyId is null or ip.insurance_company_id = :insuranceCompanyId)
                  and ct.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("referenceDate", referenceDate);
        query.setParameter("dueDateLimit", dueDateLimit);
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("insuranceCompanyId", insuranceCompanyId == null ? null : insuranceCompanyId.toString());
        bindAccessScope(query, accessScope);

        Object[] row = (Object[]) query.getSingleResult();
        if (row == null) {
            return ContractSummaryResponse.empty();
        }

        return ContractSummaryResponse.builder()
                .totalContractCount(toLong(row[0]))
                .normalPaymentCount(toLong(row[1]))
                .unpaidCount(toLong(row[2]))
                .lapseExpectedCount(toLong(row[3]))
                .expiringSoonCount(toLong(row[4]))
                .renewalSoonCount(toLong(row[5]))
                .build();
    }

    @Override
    public Page<ContractListItemResponse> searchHoldingContracts(AccessScope accessScope,
                                                                 String organizationCode,
                                                                 UUID insuranceCompanyId,
                                                                 ContractListStatus contractStatus,
                                                                 ContractListSort sort,
                                                                 LocalDate referenceDate,
                                                                 LocalDate dueDateLimit,
                                                                 Pageable pageable) {
        String fromWhereSql = buildContractListFromWhereSql(accessScope, contractStatus);
        String contentSql = """
                select
                    ct.id,
                    c.customer_name,
                    ct.contract_code,
                    ct.contract_date,
                    ct.contract_end_date,
                    ct.contract_status,
                    case when ct.contract_status = 'MAINTENANCE' then 'PAID' else 'UNPAID' end as payment_status,
                    ip.is_renewable,
                    ct.monthly_premium,
                    ip.insurance_product_name,
                    ic.insurance_company_name
                """ + fromWhereSql + buildContractListOrderBySql(sort);

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        bindContractListParameters(
                contentQuery,
                accessScope,
                organizationCode,
                insuranceCompanyId,
                contractStatus,
                referenceDate,
                dueDateLimit
        );
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<ContractListItemResponse> content = rows.stream()
                .map(row -> toContractListItemResponse(row, contractStatus))
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindContractListParameters(
                countQuery,
                accessScope,
                organizationCode,
                insuranceCompanyId,
                contractStatus,
                referenceDate,
                dueDateLimit
        );

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public List<InsuranceCompanyContractStatusResponse> summarizeInsuranceCompanyContractStatuses(
            AccessScope accessScope,
            String organizationCode,
            UUID insuranceCompanyId
    ) {
        String sql = """
                select
                    ic.insurance_company_name,
                    count(*) as total_contract_count,
                    coalesce(sum(ct.monthly_premium), 0) as total_monthly_premium_amount,
                    round(
                        coalesce(
                            sum(case when ct.contract_status = 'MAINTENANCE' then 1 else 0 end)
                            / nullif(count(*), 0) * 100,
                            0
                        ),
                        1
                    ) as retention_rate
                from contracts ct
                join customers c on c.id = ct.customer_id
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                where ct.contract_status in ('MAINTENANCE', 'LAPSED')
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:insuranceCompanyId is null or ip.insurance_company_id = :insuranceCompanyId)
                  and ct.deleted_at is null
                  and c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope) + """
                group by ic.id, ic.insurance_company_name
                order by total_contract_count desc, ic.insurance_company_name asc
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("insuranceCompanyId", insuranceCompanyId == null ? null : insuranceCompanyId.toString());
        bindAccessScope(query, accessScope);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toInsuranceCompanyContractStatusResponse)
                .toList();
    }

    @Override
    public List<ContractMonthlyTrendResponse> summarizeMonthlyContractTrend(
            AccessScope accessScope,
            String organizationCode,
            UUID insuranceCompanyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String sql = """
                select
                    date_format(ct.contract_date, '%Y-%m') as contract_month,
                    count(*) as contract_count,
                    coalesce(sum(ct.monthly_premium), 0) as total_monthly_premium_amount
                from contracts ct
                join customers c on c.id = ct.customer_id
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                where ct.contract_date between :startDate and :endDate
                  and ct.contract_status in ('MAINTENANCE', 'LAPSED')
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:insuranceCompanyId is null or ip.insurance_company_id = :insuranceCompanyId)
                  and ct.deleted_at is null
                  and c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope) + """
                group by date_format(ct.contract_date, '%Y-%m')
                order by contract_month asc
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("insuranceCompanyId", insuranceCompanyId == null ? null : insuranceCompanyId.toString());
        bindAccessScope(query, accessScope);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toContractMonthlyTrendResponse)
                .toList();
    }

    @Override
    public Optional<ContractDetailQueryResult> findContractDetail(AccessScope accessScope,
                                                                  UUID contractId) {
        String sql = """
                select
                    ct.contract_code,
                    ct.contract_status,
                    c.customer_name,
                    c.customer_status,
                    c.customer_gender,
                    c.customer_birth_date,
                    c.customer_phone,
                    c.customer_email,
                    case
                        when c.customer_address_detail is null or c.customer_address_detail = ''
                            then c.customer_address_road
                        else concat(c.customer_address_road, ', ', c.customer_address_detail)
                    end as customer_address,
                    c.customer_job,
                    c.customer_company_name,
                    ic.insurance_company_name,
                    icat.insurance_category_name,
                    ip.insurance_product_name,
                    ct.contract_date,
                    ct.contract_start_date,
                    ct.contract_end_date,
                    ct.coverage_start_date,
                    ct.coverage_end_date,
                    ct.payment_period_years,
                    ct.payment_cycle,
                    ct.monthly_premium,
                    ct.coverage_summary,
                    fp.user_name,
                    org.organization_name,
                    ct.created_at
                from contracts ct
                join customers c on c.id = ct.customer_id
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                join insurance_categories icat on icat.id = ip.insurance_category_id
                where ct.id = :contractId
                  and ct.deleted_at is null
                  and c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                """ + buildContractDetailAccessScopeWhereClause(accessScope);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("contractId", contractId.toString());
        bindAccessScope(query, accessScope);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(this::toContractDetailQueryResult);
    }

    private String buildAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "\n  and ct.fp_id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "\n  and org.id = :organizationId\n";
        }

        return "\n";
    }

    private String buildContractDetailAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "\n  and ct.fp_id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "\n  and org.id = :organizationId\n";
        }

        return "\n";
    }

    private String buildContractListFromWhereSql(AccessScope accessScope,
                                                 ContractListStatus contractStatus) {
        return """
                from contracts ct
                join customers c on c.id = ct.customer_id
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                where ct.contract_status in ('MAINTENANCE', 'LAPSED')
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:insuranceCompanyId is null or ip.insurance_company_id = :insuranceCompanyId)
                  and ct.deleted_at is null
                  and c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope)
                + buildContractStatusWhereClause(contractStatus);
    }

    private String buildContractStatusWhereClause(ContractListStatus contractStatus) {
        if (contractStatus == null) {
            return "";
        }

        return switch (contractStatus) {
            case PAID -> """
                    and ct.contract_status = 'MAINTENANCE'
                    """;
            case UNPAID -> """
                    and ct.contract_status = 'LAPSED'
                    """;
            case EXPIRING_SOON -> """
                    and ct.contract_status = 'MAINTENANCE'
                    and ip.is_renewable = false
                    and ct.contract_end_date between :referenceDate and :dueDateLimit
                    """;
            case RENEWAL_SOON -> """
                    and ct.contract_status = 'MAINTENANCE'
                    and ip.is_renewable = true
                    and ct.contract_end_date between :referenceDate and :dueDateLimit
                    """;
        };
    }

    private String buildContractListOrderBySql(ContractListSort sort) {
        ContractListSort resolvedSort = sort == null ? ContractListSort.LATEST_CONTRACT : sort;

        return switch (resolvedSort) {
            case OLDEST_CONTRACT -> "\norder by ct.contract_date asc, ct.contract_code asc\n";
            case EXPIRY_SOON -> "\norder by ct.contract_end_date asc, ct.contract_date desc, ct.contract_code asc\n";
            case LATEST_CONTRACT -> "\norder by ct.contract_date desc, ct.contract_code desc\n";
        };
    }

    private void bindContractListParameters(Query query,
                                            AccessScope accessScope,
                                            String organizationCode,
                                            UUID insuranceCompanyId,
                                            ContractListStatus contractStatus,
                                            LocalDate referenceDate,
                                            LocalDate dueDateLimit) {
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("insuranceCompanyId", insuranceCompanyId == null ? null : insuranceCompanyId.toString());
        if (contractStatus == ContractListStatus.EXPIRING_SOON || contractStatus == ContractListStatus.RENEWAL_SOON) {
            query.setParameter("referenceDate", referenceDate);
            query.setParameter("dueDateLimit", dueDateLimit);
        }
        bindAccessScope(query, accessScope);
    }

    private void bindAccessScope(Query query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId().toString());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId().toString());
        }
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(Objects.requireNonNull(value).toString());
    }

    private InsuranceCompanyContractStatusResponse toInsuranceCompanyContractStatusResponse(Object[] row) {
        return InsuranceCompanyContractStatusResponse.builder()
                .insuranceCompanyName((String) row[0])
                .totalContractCount(toLong(row[1]))
                .totalMonthlyPremiumAmount(toBigDecimal(row[2]))
                .retentionRate(toBigDecimal(row[3]))
                .build();
    }

    private ContractMonthlyTrendResponse toContractMonthlyTrendResponse(Object[] row) {
        return ContractMonthlyTrendResponse.builder()
                .month((String) row[0])
                .contractCount(toLong(row[1]))
                .totalMonthlyPremiumAmount(toBigDecimal(row[2]))
                .build();
    }

    private ContractListItemResponse toContractListItemResponse(Object[] row,
                                                                ContractListStatus requestedStatus) {
        return ContractListItemResponse.builder()
                .contractId(UUID.fromString((String) row[0]))
                .customerName((String) row[1])
                .contractCode((String) row[2])
                .contractDate(toLocalDate(row[3]))
                .contractEndDate(toLocalDate(row[4]))
                .contractStatus(resolveDisplayContractStatus(
                        (String) row[5],
                        (String) row[6],
                        toBoolean(row[7]),
                        toLocalDate(row[4]),
                        requestedStatus
                ))
                .monthlyPremium((BigDecimal) row[8])
                .insuranceProductName((String) row[9])
                .insuranceCompanyName((String) row[10])
                .build();
    }

    private String resolveDisplayContractStatus(String contractStatus,
                                                String paymentStatus,
                                                boolean renewable,
                                                LocalDate contractEndDate,
                                                ContractListStatus requestedStatus) {
        if (requestedStatus == ContractListStatus.EXPIRING_SOON) {
            return "만기임박";
        }

        if (requestedStatus == ContractListStatus.RENEWAL_SOON) {
            return "갱신임박";
        }

        if ("LAPSED".equals(contractStatus)) {
            return "실효";
        }

        if ("UNPAID".equals(paymentStatus)) {
            return "미수금";
        }

        if ("PAID".equals(paymentStatus)) {
            return "수금";
        }

        if ("MAINTENANCE".equals(contractStatus)
                && contractEndDate != null
                && !contractEndDate.isAfter(LocalDate.now().plusDays(30))) {
            return renewable ? "갱신임박" : "만기임박";
        }

        return contractStatus;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        return (LocalDate) value;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return (LocalDateTime) value;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return ((Number) value).intValue() == 1;
    }

    private ContractDetailQueryResult toContractDetailQueryResult(Object[] row) {
        return new ContractDetailQueryResult(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toLocalDate(row[5]),
                (String) row[6],
                (String) row[7],
                (String) row[8],
                (String) row[9],
                (String) row[10],
                (String) row[11],
                (String) row[12],
                (String) row[13],
                toLocalDate(row[14]),
                toLocalDate(row[15]),
                toLocalDate(row[16]),
                toLocalDate(row[17]),
                toLocalDate(row[18]),
                ((Number) row[19]).intValue(),
                (String) row[20],
                (BigDecimal) row[21],
                (String) row[22],
                (String) row[23],
                (String) row[24],
                toLocalDateTime(row[25])
        );
    }
  
    @Override
    public List<CustomerOwnedContractResponse> findOwnCustomerContracts(UUID customerId) {
        String jpql = """
                select new com.linker.relia.customer.dto.CustomerOwnedContractResponse(
                    ct.id,
                    ic.insuranceCompanyName,
                    ip.insuranceProductName,
                    ct.monthlyPremium,
                    ct.contractStartDate,
                    ct.contractStatus
                )
                from Contract ct
                join ct.insuranceProduct ip
                join ip.insuranceCompany ic
                where ct.customer.id = :customerId
                  and ct.deletedAt is null
                  and ip.deletedAt is null
                  and ic.deletedAt is null
                order by ct.contractStartDate desc, ct.createdAt desc
                """;

        TypedQuery<CustomerOwnedContractResponse> query =
                entityManager.createQuery(jpql, CustomerOwnedContractResponse.class);
        query.setParameter("customerId", customerId);
        return query.getResultList();
    }
}
