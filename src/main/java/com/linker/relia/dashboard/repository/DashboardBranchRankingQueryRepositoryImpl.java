package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardBranchRankingItemResponse;
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
public class DashboardBranchRankingQueryRepositoryImpl implements DashboardBranchRankingQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<DashboardBranchRankingItemResponse> findBranchRankings(
            String closingMonth,
            String comparisonClosingMonth,
            DashboardRankOrder rankOrder,
            Pageable pageable
    ) {
        Query contentQuery = entityManager.createNativeQuery("""
                with current_ranked as (
                    select
                        bicmc.organization_id,
                        bicmc.net_income_commission_amount,
                        rank() over (
                            order by bicmc.net_income_commission_amount desc
                        ) as ranking
                    from branch_income_commission_monthly_closing bicmc
                    where bicmc.closing_month = :closingMonth
                ),
                previous_ranked as (
                    select
                        bicmc.organization_id,
                        rank() over (
                            order by bicmc.net_income_commission_amount desc
                        ) as ranking
                    from branch_income_commission_monthly_closing bicmc
                    where bicmc.closing_month = :comparisonClosingMonth
                )
                select
                    curr.ranking,
                    curr.organization_id,
                    omc.organization_code,
                    omc.organization_name,
                    curr.net_income_commission_amount,
                    prev.ranking as previous_rank,
                    case
                        when prev.ranking is null then null
                        else prev.ranking - curr.ranking
                    end as rank_change
                from current_ranked curr
                join organization_monthly_closing omc
                  on omc.organization_id = curr.organization_id
                 and omc.closing_month = :closingMonth
                 and omc.organization_type = 'BRANCH'
                left join previous_ranked prev
                  on prev.organization_id = curr.organization_id
                order by curr.ranking %s, omc.organization_name asc, curr.organization_id asc
                """.formatted(rankOrder.sqlDirection()));
        bindParameters(contentQuery, closingMonth, comparisonClosingMonth);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<DashboardBranchRankingItemResponse> content = rows.stream()
                .map(this::toResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("""
                select count(*)
                from branch_income_commission_monthly_closing bicmc
                join organization_monthly_closing omc
                  on omc.organization_id = bicmc.organization_id
                 and omc.closing_month = bicmc.closing_month
                 and omc.organization_type = 'BRANCH'
                where bicmc.closing_month = :closingMonth
                """);
        countQuery.setParameter("closingMonth", closingMonth);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public Optional<String> findLatestClosingMonth() {
        Query query = entityManager.createNativeQuery("""
                select max(bicmc.closing_month)
                from branch_income_commission_monthly_closing bicmc
                """);
        return Optional.ofNullable((String) query.getSingleResult());
    }

    private void bindParameters(Query query, String closingMonth, String comparisonClosingMonth) {
        query.setParameter("closingMonth", closingMonth);
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);
    }

    private DashboardBranchRankingItemResponse toResponse(Object[] row) {
        Integer previousRank = toNullableInt(row[5]);
        return DashboardBranchRankingItemResponse.builder()
                .rank(toInt(row[0]))
                .organizationId(toUuid(row[1]))
                .organizationCode((String) row[2])
                .organizationName((String) row[3])
                .netIncomeCommissionAmount(toBigDecimal(row[4]))
                .previousRank(previousRank)
                .rankChange(toNullableInt(row[6]))
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

    private Integer toNullableInt(Object value) {
        return value == null ? null : ((Number) value).intValue();
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
