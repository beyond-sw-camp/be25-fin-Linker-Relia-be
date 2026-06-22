package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.FpContractListItemResponse;
import com.linker.relia.organization.dto.FpDetailResponse;
import com.linker.relia.organization.dto.FpListItemResponse;
import com.linker.relia.organization.dto.FpMonthlyPerformanceItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrganizationFpRepositoryImpl implements OrganizationFpRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<FpListItemResponse> searchFps(AccessScope accessScope,
                                              String keyword,
                                              UUID organizationId,
                                              String closingMonth,
                                              Pageable pageable) {
        String fromWhereSql = buildFromWhereSql(accessScope);
        String contentSql = """
                select
                    fp.id,
                    fp.emp_code,
                    fp.user_name,
                    org.id as organization_id,
                    org.organization_name,
                    coalesce(:closingMonth, fmi.closing_month) as closing_month,
                    case
                        when coalesce(:closingMonth, fmi.closing_month) is null then (
                            select count(*)
                            from customers c
                            where c.customer_fp_id = fp.id
                              and c.deleted_at is null
                        )
                        else (
                            select count(distinct cmc.customer_id)
                            from contract_monthly_closing cmc
                            join contracts ct on ct.id = cmc.contract_id
                            join customers c on c.id = cmc.customer_id
                            where cmc.fp_id = fp.id
                              and cmc.closing_month = coalesce(:closingMonth, fmi.closing_month)
                              and ct.deleted_at is null
                              and c.deleted_at is null
                        )
                    end as customer_count,
                    case
                        when coalesce(:closingMonth, fmi.closing_month) is null then (
                            select count(*)
                            from contracts ct
                            where ct.fp_id = fp.id
                              and ct.deleted_at is null
                        )
                        else (
                            select count(*)
                            from contract_monthly_closing cmc
                            join contracts ct on ct.id = cmc.contract_id
                            join customers c on c.id = cmc.customer_id
                            where cmc.fp_id = fp.id
                              and cmc.closing_month = coalesce(:closingMonth, fmi.closing_month)
                              and ct.deleted_at is null
                              and c.deleted_at is null
                        )
                    end as contract_count,
                    coalesce(
                        fmi.retention_rate,
                        case
                            when coalesce(:closingMonth, fmi.closing_month) is null then (
                                select round(coalesce(sum(case when ct.contract_status = 'MAINTENANCE' then 1 else 0 end) / nullif(count(*), 0) * 100, 0), 2)
                                from contracts ct
                                where ct.fp_id = fp.id
                                  and ct.deleted_at is null
                            )
                            else (
                                select round(coalesce(sum(case when cmc.contract_status = 'MAINTENANCE' then 1 else 0 end) / nullif(count(*), 0) * 100, 0), 2)
                                from contract_monthly_closing cmc
                                join contracts ct on ct.id = cmc.contract_id
                                join customers c on c.id = cmc.customer_id
                                where cmc.fp_id = fp.id
                                  and cmc.closing_month = coalesce(:closingMonth, fmi.closing_month)
                                  and ct.deleted_at is null
                                  and c.deleted_at is null
                            )
                        end,
                        0
                    ) as retention_rate
                """ + fromWhereSql + """
                order by org.organization_name asc, fp.user_name asc, fp.emp_code asc
                """;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        bindParameters(contentQuery, accessScope, keyword, organizationId, closingMonth);
        if (pageable.isPaged()) {
            contentQuery.setFirstResult((int) pageable.getOffset());
            contentQuery.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<FpListItemResponse> content = rows.stream()
                .map(this::toFpListItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindParameters(countQuery, accessScope, keyword, organizationId, closingMonth);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public Optional<FpDetailResponse> findFpDetail(AccessScope accessScope, UUID fpId, String closingMonth) {
        String sql = """
                select
                    fp.id,
                    fp.emp_code,
                    fp.user_name,
                    org.id as organization_id,
                    org.organization_name,
                    fp.phone,
                    fp.email,
                    fp.joined_at,
                    fmpc.closing_month,
                    fmpc.completed_contract_count,
                    fmpc.new_contract_count,
                    fmpc.retention_rate,
                    fmpc.total_rank,
                    fmpc.branch_rank
                from users fp
                join organizations org on org.id = fp.organization_id
                left join fp_monthly_performance_closing fmpc
                  on fmpc.fp_id = fp.id
                 and fmpc.closing_month = coalesce(
                     :closingMonth,
                     (
                         select max(fmpc2.closing_month)
                         from fp_monthly_performance_closing fmpc2
                         where fmpc2.fp_id = fp.id
                     )
                 )
                where fp.id = :fpId
                  and fp.user_role = 'FP'
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope);

        Query query = entityManager.createNativeQuery(sql);
        bindFpDetailParameters(query, accessScope, fpId, closingMonth);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(this::toFpDetailResponse);
    }

    @Override
    public List<FpMonthlyPerformanceItemResponse> findFpMonthlyPerformances(AccessScope accessScope,
                                                                           UUID fpId,
                                                                           String fromClosingMonth,
                                                                           String toClosingMonth) {
        String sql = """
                select
                    fmpc.closing_month,
                    fmpc.completed_contract_count,
                    fmpc.new_contract_count,
                    fmpc.retention_rate,
                    fmpc.total_rank,
                    fmpc.branch_rank
                from fp_monthly_performance_closing fmpc
                join users fp on fp.id = fmpc.fp_id
                join organizations org on org.id = fmpc.organization_id
                where fp.id = :fpId
                  and fp.user_role = 'FP'
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and (:fromClosingMonth is null or fmpc.closing_month >= :fromClosingMonth)
                  and (:toClosingMonth is null or fmpc.closing_month <= :toClosingMonth)
                """ + buildAccessScopeWhereClause(accessScope) + """
                order by fmpc.closing_month asc
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("fromClosingMonth", fromClosingMonth);
        query.setParameter("toClosingMonth", toClosingMonth);
        bindAccessScopeParameters(query, accessScope);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toFpMonthlyPerformanceItemResponse)
                .toList();
    }

    @Override
    public boolean existsFp(UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from users fp
                join organizations org on org.id = fp.organization_id
                where fp.id = :fpId
                  and fp.user_role = 'FP'
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """);
        query.setParameter("fpId", fpId.toString());

        return toLong(query.getSingleResult()) > 0;
    }

    @Override
    public boolean existsFpInScope(AccessScope accessScope, UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from users fp
                join organizations org on org.id = fp.organization_id
                where fp.id = :fpId
                  and fp.user_role = 'FP'
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope));
        query.setParameter("fpId", fpId.toString());
        bindAccessScopeParameters(query, accessScope);

        return toLong(query.getSingleResult()) > 0;
    }

    @Override
    public Page<FpContractListItemResponse> findFpContracts(AccessScope accessScope,
                                                            UUID fpId,
                                                            Pageable pageable) {
        String fromWhereSql = """
                from contracts ct
                join customers c on c.id = ct.customer_id
                join users fp on fp.id = ct.fp_id
                join organizations org on org.id = fp.organization_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                join insurance_companies ic on ic.id = ip.insurance_company_id
                join insurance_categories icat on icat.id = ip.insurance_category_id
                where fp.id = :fpId
                  and ct.deleted_at is null
                  and c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and ip.deleted_at is null
                  and ic.deleted_at is null
                  and icat.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope);

        String contentSql = """
                select
                    c.customer_name,
                    icat.insurance_category_name,
                    ic.insurance_company_name,
                    ct.contract_date,
                    ct.monthly_premium,
                    ct.contract_status
                """ + fromWhereSql + """
                order by ct.contract_date desc, ct.contract_code desc
                """;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        bindFpContractParameters(contentQuery, accessScope, fpId);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<FpContractListItemResponse> content = rows.stream()
                .map(this::toFpContractListItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindFpContractParameters(countQuery, accessScope, fpId);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    private String buildFromWhereSql(AccessScope accessScope) {
        return """
                from users fp
                join organizations org on org.id = fp.organization_id
                left join fp_monthly_info fmi
                  on fmi.emp_code = fp.emp_code
                 and fmi.closing_month = coalesce(
                     :closingMonth,
                     (
                         select max(fmi2.closing_month)
                         from fp_monthly_info fmi2
                         where fmi2.emp_code = fp.emp_code
                     )
                 )
                where fp.user_role = 'FP'
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and (:keyword is null or fp.user_name like concat('%', :keyword, '%'))
                  and (:organizationId is null or org.id = :organizationId)
                """ + buildAccessScopeWhereClause(accessScope);
    }

    private String buildAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "  and fp.id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "  and org.id = :accessOrganizationId\n";
        }

        return "";
    }

    private void bindParameters(Query query,
                                AccessScope accessScope,
                                String keyword,
                                UUID organizationId,
                                String closingMonth) {
        query.setParameter("keyword", keyword);
        query.setParameter("organizationId", organizationId == null ? null : organizationId.toString());
        query.setParameter("closingMonth", closingMonth);

        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId().toString());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("accessOrganizationId", accessScope.organizationId().toString());
        }
    }

    private void bindFpDetailParameters(Query query,
                                        AccessScope accessScope,
                                        UUID fpId,
                                        String closingMonth) {
        query.setParameter("fpId", fpId.toString());
        query.setParameter("closingMonth", closingMonth);

        bindAccessScopeParameters(query, accessScope);
    }

    private void bindFpContractParameters(Query query,
                                          AccessScope accessScope,
                                          UUID fpId) {
        query.setParameter("fpId", fpId.toString());
        bindAccessScopeParameters(query, accessScope);
    }

    private void bindAccessScopeParameters(Query query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId().toString());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("accessOrganizationId", accessScope.organizationId().toString());
        }
    }

    private FpListItemResponse toFpListItemResponse(Object[] row) {
        return FpListItemResponse.builder()
                .id(toUuid(row[0]))
                .empCode((String) row[1])
                .userName((String) row[2])
                .organizationId(toUuid(row[3]))
                .organizationName((String) row[4])
                .closingMonth((String) row[5])
                .customerCount(toLong(row[6]))
                .contractCount(toLong(row[7]))
                .retentionRate(toBigDecimal(row[8]))
                .build();
    }

    private FpContractListItemResponse toFpContractListItemResponse(Object[] row) {
        return FpContractListItemResponse.builder()
                .customerName((String) row[0])
                .insuranceType((String) row[1])
                .insuranceCompany((String) row[2])
                .contractDate(toLocalDate(row[3]))
                .monthlyPremium(toBigDecimal(row[4]))
                .contractStatus(toContractStatusLabel((String) row[5]))
                .build();
    }

    private String toContractStatusLabel(String contractStatus) {
        if ("MAINTENANCE".equals(contractStatus)) {
            return "유지";
        }

        if ("LAPSED".equals(contractStatus)) {
            return "실효";
        }

        if ("TERMINATED".equals(contractStatus)) {
            return "해지";
        }

        if ("COMPLETED".equals(contractStatus)) {
            return "만기";
        }

        return contractStatus;
    }

    private FpDetailResponse toFpDetailResponse(Object[] row) {
        return FpDetailResponse.builder()
                .fpId(toUuid(row[0]))
                .empCode((String) row[1])
                .fpName((String) row[2])
                .organizationId(toUuid(row[3]))
                .organizationName((String) row[4])
                .phone((String) row[5])
                .email((String) row[6])
                .hireDate(toLocalDate(row[7]))
                .performanceSummary(toPerformanceSummary(row))
                .build();
    }

    private FpDetailResponse.PerformanceSummary toPerformanceSummary(Object[] row) {
        if (row[8] == null) {
            return null;
        }

        return FpDetailResponse.PerformanceSummary.builder()
                .closingMonth((String) row[8])
                .completedContractCount(toInt(row[9]))
                .newContractCount(toInt(row[10]))
                .retentionRate(toBigDecimal(row[11]))
                .totalRank(toInt(row[12]))
                .branchRank(toInt(row[13]))
                .build();
    }

    private FpMonthlyPerformanceItemResponse toFpMonthlyPerformanceItemResponse(Object[] row) {
        return FpMonthlyPerformanceItemResponse.builder()
                .closingMonth((String) row[0])
                .completedContractCount(toInt(row[1]))
                .newContractCount(toInt(row[2]))
                .retentionRate(toBigDecimal(row[3]))
                .totalRank(toInt(row[4]))
                .branchRank(toInt(row[5]))
                .build();
    }

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        return LocalDate.parse(value.toString());
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private int toInt(Object value) {
        return ((Number) Objects.requireNonNull(value)).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
