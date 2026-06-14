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

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
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
}
