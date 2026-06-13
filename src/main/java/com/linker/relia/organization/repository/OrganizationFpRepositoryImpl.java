package com.linker.relia.organization.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.organization.dto.FpListItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class OrganizationFpRepositoryImpl implements OrganizationFpRepository {
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Searches FP (financial planner) records with optional filtering, closing-month override and pagination.
     *
     * The search respects the provided access scope, filters by `keyword` and `organizationId` when present,
     * and uses `closingMonth` to override per-FP monthly data computations (customer/contract counts and retention rate).
     *
     * @param accessScope   scope that limits which FP records are visible (e.g. own, branch, or all)
     * @param keyword       optional substring to match against FP user names; pass `null` to disable
     * @param organizationId optional organization UUID to restrict results; pass `null` to disable
     * @param closingMonth  optional closing month (formatted string) to use for monthly aggregates; pass `null` to use each FP's latest applicable month
     * @param pageable      pagination and sorting information
     * @return              a Page of FpListItemResponse containing the matching FP records and total count for pagination
     */
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
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<FpListItemResponse> content = rows.stream()
                .map(this::toFpListItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindParameters(countQuery, accessScope, keyword, organizationId, closingMonth);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    /**
     * Builds the SQL "FROM ... WHERE" fragment used to query FP records, including joins to organizations and FP monthly info and the base filters.
     *
     * @param accessScope controls which access-scope constraint (own, branch, or none) is appended to the returned fragment
     * @return a SQL fragment beginning with `from` that joins `users` to `organizations`, left-joins `fp_monthly_info` (using the provided `:closingMonth` or the latest closing month per employee when `:closingMonth` is null), and applies base WHERE predicates for FP role, deletion flags, optional keyword and organization filters, plus the access-scope clause
     */
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

    /**
     * Builds an SQL WHERE-clause fragment that enforces the provided access scope.
     *
     * @param accessScope the access scope determining which constraint to apply (own, branch, or global)
     * @return "`and fp.id = :userId\n`" when the scope is own, "`and org.id = :accessOrganizationId\n`" when the scope is branch, or an empty string when no access constraint is required
     */
    private String buildAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "  and fp.id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "  and org.id = :accessOrganizationId\n";
        }

        return "";
    }

    /**
     * Bind search parameters and access-scope identifiers into the given JPA query.
     *
     * Sets "keyword", "organizationId" (as a string or `null`), and "closingMonth".
     * Additionally sets "userId" when `accessScope.isOwnScope()` is true or
     * "accessOrganizationId" when `accessScope.isBranchScope()` is true.
     *
     * @param query the JPA Query to bind parameters on
     * @param accessScope access control descriptor whose identifiers may be bound
     * @param keyword optional search keyword for user name filtering
     * @param organizationId optional organization id; bound as its string representation or `null`
     * @param closingMonth optional closing month to use when computing monthly metrics
     */
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

    /**
     * Create an FpListItemResponse from a single result row produced by the native FP query.
     *
     * @param row the result row as an Object[] with expected columns in order:
     *            0 - id (UUID string or null),
     *            1 - emp_code (String),
     *            2 - user_name (String),
     *            3 - organization id (UUID string or null),
     *            4 - organization_name (String),
     *            5 - closing_month (String),
     *            6 - customer_count (Number),
     *            7 - contract_count (Number),
     *            8 - retention_rate (BigDecimal or numeric)
     * @return an FpListItemResponse populated from the corresponding row values
     */
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

    /**
     * Convert an object to a UUID, returning null when the input is null.
     *
     * @param value an object containing a UUID string representation or null
     * @return the parsed UUID, or null if {@code value} is null
     */
    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    /**
     * Converts a non-null numeric object to its primitive long value.
     *
     * @param value the numeric value to convert; must be an instance of {@link Number}
     * @return the long value of the given number
     * @throws NullPointerException if {@code value} is null
     * @throws ClassCastException if {@code value} is not a {@link Number}
     */
    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    /**
     * Convert an object to a BigDecimal.
     *
     * If the input is already a BigDecimal it is returned unchanged; otherwise a new
     * BigDecimal is created from the input's toString() representation.
     *
     * @param value the value to convert
     * @return the corresponding BigDecimal
     * @throws NullPointerException if {@code value} is null
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
