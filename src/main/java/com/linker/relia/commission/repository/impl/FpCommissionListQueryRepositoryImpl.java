package com.linker.relia.commission.repository.impl;

import com.linker.relia.commission.dto.FpCommissionListQueryResult;
import com.linker.relia.commission.repository.custom.FpCommissionListQueryRepository;
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
public class FpCommissionListQueryRepositoryImpl implements FpCommissionListQueryRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<FpCommissionListQueryResult> findBranchFpCommissionList(String closingMonth, UUID organizationId, Pageable pageable) {
        String fromWhereSql = """
                from fp_commission_monthly_closing fcmc
                join users fp on fp.id = fcmc.fp_id
                join organizations org on org.id = fcmc.organization_id
                where fcmc.closing_month = :closingMonth
                  and fcmc.organization_id = :organizationId
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """;

        Query query = entityManager.createNativeQuery("""
                select
                    fp.id,
                    fp.user_name,
                    fcmc.total_initial_payment_amount,
                    fcmc.total_maintenance_payment_amount,
                    fcmc.total_recovery_collection_amount,
                    fcmc.total_payment_amount,
                    fcmc.net_commission_amount,
                    fcmc.contract_count,
                    fcmc.recovery_contract_count
                """ + fromWhereSql);
        query.setParameter("closingMonth", closingMonth);
        query.setParameter("organizationId", organizationId.toString());
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<FpCommissionListQueryResult> content = rows.stream()
                .map(this::toQueryResult)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        countQuery.setParameter("closingMonth", closingMonth);
        countQuery.setParameter("organizationId", organizationId.toString());

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public Page<FpCommissionListQueryResult> findHqFpCommissionList(String closingMonth, Pageable pageable) {
        String fromWhereSql = """
                from fp_commission_monthly_closing fcmc
                join users fp on fp.id = fcmc.fp_id
                join organizations org on org.id = fcmc.organization_id
                where fcmc.closing_month = :closingMonth
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """;

        Query query = entityManager.createNativeQuery("""
                select
                    fp.id,
                    fp.user_name,
                    fcmc.total_initial_payment_amount,
                    fcmc.total_maintenance_payment_amount,
                    fcmc.total_recovery_collection_amount,
                    fcmc.total_payment_amount,
                    fcmc.net_commission_amount,
                    fcmc.contract_count,
                    fcmc.recovery_contract_count
                """ + fromWhereSql);
        query.setParameter("closingMonth", closingMonth);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<FpCommissionListQueryResult> content = rows.stream()
                .map(this::toQueryResult)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        countQuery.setParameter("closingMonth", closingMonth);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    private FpCommissionListQueryResult toQueryResult(Object[] row) {
        return new FpCommissionListQueryResult(
                toUuid(row[0]),
                (String) row[1],
                toBigDecimal(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toLong(row[7]),
                toLong(row[8])
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
