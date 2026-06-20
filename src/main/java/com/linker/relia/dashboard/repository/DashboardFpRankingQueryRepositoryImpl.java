package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardFpRankingItemResponse;
import com.linker.relia.dashboard.dto.DashboardRankOrder;
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
import java.util.Optional;
import java.util.UUID;

@Repository
public class DashboardFpRankingQueryRepositoryImpl implements DashboardFpRankingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<DashboardFpRankingItemResponse> findFpRankings(
            String closingMonth,
            UUID organizationId,
            DashboardRankOrder rankOrder,
            Pageable pageable
    ) {
        String organizationFilter = organizationId == null
                ? ""
                : "\n  and fmpc.organization_id = :organizationId\n";
        String rankColumn = organizationId == null ? "fmpc.total_rank" : "fmpc.branch_rank";

        String fromWhereClause = """
                from fp_monthly_performance_closing fmpc
                join users fp on fp.id = fmpc.fp_id
                join organizations org on org.id = fmpc.organization_id
                where fmpc.closing_month = :closingMonth
                """ + organizationFilter;

        Query contentQuery = entityManager.createNativeQuery("""
                select
                    %s as ranking,
                    fp.id as fp_id,
                    fp.emp_code,
                    fp.user_name,
                    org.organization_code,
                    org.organization_name,
                    fmpc.new_contract_count,
                    fmpc.completed_contract_count,
                    fmpc.retention_rate,
                    fmpc.customer_count,
                    fmpc.commission_amount
                """.formatted(rankColumn) + fromWhereClause + """
                order by ranking %s, fp.user_name asc, fp.id asc
                """.formatted(rankOrder.sqlDirection()));
        bindParameters(contentQuery, closingMonth, organizationId);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<DashboardFpRankingItemResponse> content = rows.stream()
                .map(this::toResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereClause);
        bindParameters(countQuery, closingMonth, organizationId);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public Optional<String> findLatestClosingMonth() {
        Query query = entityManager.createNativeQuery("""
                select max(fmpc.closing_month)
                from fp_monthly_performance_closing fmpc
                """);
        return Optional.ofNullable((String) query.getSingleResult());
    }

    private void bindParameters(Query query, String closingMonth, UUID organizationId) {
        query.setParameter("closingMonth", closingMonth);
        if (organizationId != null) {
            query.setParameter("organizationId", organizationId.toString());
        }
    }

    private DashboardFpRankingItemResponse toResponse(Object[] row) {
        return DashboardFpRankingItemResponse.builder()
                .rank(toInt(row[0]))
                .fpId(toUuid(row[1]))
                .empCode((String) row[2])
                .fpName((String) row[3])
                .organizationCode((String) row[4])
                .organizationName((String) row[5])
                .newContractCount(toInt(row[6]))
                .managedContractCount(toInt(row[7]))
                .retentionRate(toBigDecimal(row[8]))
                .customerCount(toInt(row[9]))
                .commissionAmount(toBigDecimal(row[10]))
                .build();
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(Objects.requireNonNull(value).toString());
    }

    private int toInt(Object value) {
        return ((Number) Objects.requireNonNull(value)).intValue();
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
