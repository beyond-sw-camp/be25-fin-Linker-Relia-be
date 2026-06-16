package com.linker.relia.dashboard.repository;

import com.linker.relia.dashboard.dto.DashboardSummaryQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DashboardSummaryQueryRepositoryImpl implements DashboardSummaryQueryRepository {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DashboardSummaryQueryResult findFpSummary(UUID fpId,
                                                     UUID organizationId,
                                                     LocalDate currentMonthStart,
                                                     LocalDate referenceDate,
                                                     String currentMonth,
                                                     String comparisonClosingMonth) {
        return new DashboardSummaryQueryResult(
                countCurrentNewContracts(fpId, currentMonthStart, referenceDate),
                countPreviousClosedNewContracts(fpId, comparisonClosingMonth),
                calculateCurrentRetentionRate(fpId),
                findPreviousRetentionRate(fpId, comparisonClosingMonth),
                findCurrentBranchRank(fpId, organizationId, currentMonth),
                findPreviousBranchRank(fpId, organizationId, comparisonClosingMonth),
                countCurrentCustomers(fpId),
                countCurrentMonthCustomerNetIncrease(fpId, currentMonthStart, referenceDate),
                countCurrentMonthNewHandovers(fpId, currentMonthStart, referenceDate),
                countPreviousMonthNewHandovers(fpId, comparisonClosingMonth),
                calculateCurrentExpectedCommission(fpId, currentMonth),
                findPreviousClosedCommission(fpId, comparisonClosingMonth)
        );
    }

    private long countCurrentNewContracts(UUID fpId, LocalDate currentMonthStart, LocalDate referenceDate) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from contracts ct
                where ct.fp_id = :fpId
                  and ct.contract_date between :currentMonthStart and :referenceDate
                  and ct.deleted_at is null
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("currentMonthStart", currentMonthStart);
        query.setParameter("referenceDate", referenceDate);
        return toLong(query.getSingleResult());
    }

    private long countPreviousClosedNewContracts(UUID fpId, String comparisonClosingMonth) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from contract_monthly_closing cmc
                where cmc.fp_id = :fpId
                  and cmc.closing_month = :comparisonClosingMonth
                  and cmc.contract_date between
                      str_to_date(concat(:comparisonClosingMonth, '-01'), '%Y-%m-%d')
                      and last_day(str_to_date(concat(:comparisonClosingMonth, '-01'), '%Y-%m-%d'))
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);
        return toLong(query.getSingleResult());
    }

    private BigDecimal calculateCurrentRetentionRate(UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select
                    count(*) as total_contract_count,
                    coalesce(sum(case when ct.contract_status = 'MAINTENANCE' then 1 else 0 end), 0) as active_contract_count
                from contracts ct
                where ct.fp_id = :fpId
                  and ct.contract_status in ('MAINTENANCE', 'LAPSED')
                  and ct.deleted_at is null
                """);
        query.setParameter("fpId", fpId.toString());

        Object[] row = (Object[]) query.getSingleResult();
        long totalCount = toLong(row[0]);
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long activeCount = toLong(row[1]);
        return BigDecimal.valueOf(activeCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal findPreviousRetentionRate(UUID fpId, String comparisonClosingMonth) {
        Query query = entityManager.createNativeQuery("""
                select
                    count(*) as total_contract_count,
                    coalesce(sum(case when cmc.contract_status = 'MAINTENANCE' then 1 else 0 end), 0) as active_contract_count
                from contract_monthly_closing cmc
                where cmc.fp_id = :fpId
                  and cmc.closing_month = :comparisonClosingMonth
                  and cmc.contract_status in ('MAINTENANCE', 'LAPSED')
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);

        Object[] row = (Object[]) query.getSingleResult();
        long totalCount = toLong(row[0]);
        if (totalCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long activeCount = toLong(row[1]);
        return BigDecimal.valueOf(activeCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }

    private Integer findCurrentBranchRank(UUID fpId, UUID organizationId, String currentMonth) {
        Query query = entityManager.createNativeQuery("""
                select ranked.rank_value
                from (
                    select
                        fp.id as fp_id,
                        sum(coalesce(sum(case
                            when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT')
                                then pcr.commission_amount
                            when pcr.commission_type = 'RECOVERY_COLLECTION'
                                then -pcr.commission_amount
                            else 0
                        end), 0)) over () as branch_net_commission_amount,
                        rank() over (
                            order by coalesce(sum(case
                                when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT')
                                    then pcr.commission_amount
                                when pcr.commission_type = 'RECOVERY_COLLECTION'
                                    then -pcr.commission_amount
                                else 0
                            end), 0) desc, fp.user_name asc, fp.id asc
                        ) as rank_value
                    from users fp
                    left join payment_commission_records pcr
                           on pcr.fp_id = fp.id
                          and pcr.commission_month = :currentMonth
                    where fp.organization_id = :organizationId
                      and fp.user_role = 'FP'
                      and fp.deleted_at is null
                    group by fp.id, fp.user_name
                ) ranked
                where ranked.fp_id = :fpId
                  and ranked.branch_net_commission_amount > 0
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("organizationId", organizationId.toString());
        query.setParameter("currentMonth", currentMonth);

        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        return rows.isEmpty() ? null : toNullableInteger(rows.getFirst());
    }

    private Integer findPreviousBranchRank(UUID fpId, UUID organizationId, String comparisonClosingMonth) {
        Query query = entityManager.createNativeQuery("""
                select ranked.rank_value
                from (
                    select
                        fp.id as fp_id,
                        rank() over (
                            order by coalesce(max(fcmc.net_commission_amount), 0) desc, fp.user_name asc, fp.id asc
                        ) as rank_value
                    from users fp
                    left join fp_commission_monthly_closing fcmc
                           on fcmc.fp_id = fp.id
                          and fcmc.closing_month = :comparisonClosingMonth
                    where fp.organization_id = :organizationId
                      and fp.user_role = 'FP'
                      and fp.deleted_at is null
                    group by fp.id, fp.user_name
                ) ranked
                where ranked.fp_id = :fpId
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("organizationId", organizationId.toString());
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);
        return toNullableInteger(query.getSingleResult());
    }

    private long countCurrentCustomers(UUID fpId) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from customers c
                where c.customer_fp_id = :fpId
                  and c.deleted_at is null
                """);
        query.setParameter("fpId", fpId.toString());
        return toLong(query.getSingleResult());
    }

    private long countCurrentMonthCustomerNetIncrease(UUID fpId, LocalDate currentMonthStart, LocalDate referenceDate) {
        Query query = entityManager.createNativeQuery("""
                select
                    coalesce(sum(case when cfh.after_fp_id = :fpId then 1 else 0 end), 0)
                    - coalesce(sum(case when cfh.before_fp_id = :fpId then 1 else 0 end), 0)
                from customer_fp_history cfh
                where cfh.changed_at >= :currentMonthStart
                  and cfh.changed_at < date_add(:referenceDate, interval 1 day)
                  and (cfh.after_fp_id = :fpId or cfh.before_fp_id = :fpId)
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("currentMonthStart", currentMonthStart);
        query.setParameter("referenceDate", referenceDate);
        return toLong(query.getSingleResult());
    }

    private long countCurrentMonthNewHandovers(UUID fpId, LocalDate currentMonthStart, LocalDate referenceDate) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from customer_fp_history cfh
                where cfh.after_fp_id = :fpId
                  and cfh.changed_at >= :currentMonthStart
                  and cfh.changed_at < date_add(:referenceDate, interval 1 day)
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("currentMonthStart", currentMonthStart);
        query.setParameter("referenceDate", referenceDate);
        return toLong(query.getSingleResult());
    }

    private long countPreviousMonthNewHandovers(UUID fpId, String comparisonClosingMonth) {
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from customer_fp_history cfh
                where cfh.after_fp_id = :fpId
                  and cfh.changed_at >= str_to_date(concat(:comparisonClosingMonth, '-01'), '%Y-%m-%d')
                  and cfh.changed_at < date_add(last_day(str_to_date(concat(:comparisonClosingMonth, '-01'), '%Y-%m-%d')), interval 1 day)
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);
        return toLong(query.getSingleResult());
    }

    private BigDecimal calculateCurrentExpectedCommission(UUID fpId, String currentMonth) {
        Query query = entityManager.createNativeQuery("""
                select round(coalesce(sum(case
                    when pcr.commission_type in ('INITIAL_PAYMENT', 'MAINTENANCE_PAYMENT')
                        then pcr.commission_amount
                    when pcr.commission_type = 'RECOVERY_COLLECTION'
                        then -pcr.commission_amount
                    else 0
                end), 0), 2)
                from payment_commission_records pcr
                where pcr.fp_id = :fpId
                  and pcr.commission_month = :currentMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("currentMonth", currentMonth);
        return toBigDecimal(query.getSingleResult());
    }

    private BigDecimal findPreviousClosedCommission(UUID fpId, String comparisonClosingMonth) {
        Query query = entityManager.createNativeQuery("""
                select coalesce(max(fcmc.net_commission_amount), 0)
                from fp_commission_monthly_closing fcmc
                where fcmc.fp_id = :fpId
                  and fcmc.closing_month = :comparisonClosingMonth
                """);
        query.setParameter("fpId", fpId.toString());
        query.setParameter("comparisonClosingMonth", comparisonClosingMonth);
        return toBigDecimal(query.getSingleResult());
    }

    private long toLong(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.longValue();
        }
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private Integer toNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.intValue();
        }
        return ((Number) value).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(Objects.requireNonNull(value).toString());
    }
}
