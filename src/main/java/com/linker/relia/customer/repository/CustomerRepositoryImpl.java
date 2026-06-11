package com.linker.relia.customer.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.customer.domain.InterestReason;
import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.dto.CustomerDetailQueryResult;
import com.linker.relia.customer.dto.CustomerInterestItemResponse;
import com.linker.relia.customer.dto.CustomerInterestSummaryResponse;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerRepositoryImpl implements CustomerRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerListSummaryResponse summarizeCustomers(AccessScope accessScope,
                                                          String organizationCode) {
        String summaryJpql = """
                select new com.linker.relia.customer.dto.CustomerListSummaryResponse(
                    count(c),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.CONTRACTED then 1L else 0L end), 0L),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.PROSPECT then 1L else 0L end), 0L),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.COMPLETED then 1L else 0L end), 0L),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.TERMINATED then 1L else 0L end), 0L)
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildSummaryWhereClause(accessScope);

        TypedQuery<CustomerListSummaryResponse> summaryQuery =
                entityManager.createQuery(summaryJpql, CustomerListSummaryResponse.class);

        bindAccessScope(summaryQuery, accessScope);
        summaryQuery.setParameter("organizationCode", organizationCode);

        return summaryQuery.getSingleResult();
    }

    @Override
    public CustomerInterestSummaryResponse summarizeInterestCustomers(AccessScope accessScope,
                                                                      String organizationCode) {
        String sql = """
                select
                    count(*) as total_interest_customer_count,
                    coalesce(sum(case when c.interest_reason = 'UNPAID' then 1 else 0 end), 0) as unpaid_interest_customer_count,
                    coalesce(sum(case when c.interest_reason = 'RENEWAL_DUE' then 1 else 0 end), 0) as renewal_due_interest_customer_count,
                    coalesce(sum(case when c.interest_reason = 'MATURITY_DUE' then 1 else 0 end), 0) as maturity_due_interest_customer_count
                from customers c
                join users fp on fp.id = c.customer_fp_id
                join organizations org on org.id = fp.organization_id
                where c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and c.interest_yn = true
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                """ + buildInterestAccessScopeWhereClause(accessScope);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("organizationCode", organizationCode);
        bindAccessScope(query, accessScope);

        Object[] row = (Object[]) query.getSingleResult();
        return CustomerInterestSummaryResponse.builder()
                .totalInterestCustomerCount(toLong(row[0]))
                .unpaidInterestCustomerCount(toLong(row[1]))
                .renewalDueInterestCustomerCount(toLong(row[2]))
                .maturityDueInterestCustomerCount(toLong(row[3]))
                .build();
    }

    @Override
    public Page<CustomerListItemResponse> searchCustomers(AccessScope accessScope,
                                                          String customerName,
                                                          String organizationCode,
                                                          CustomerStatus customerStatus,
                                                          Pageable pageable) {
        String contentJpql = """
                select new com.linker.relia.customer.dto.CustomerListItemResponse(
                    c.id,
                    c.customerName,
                    c.customerBirthDate,
                    c.customerPhone,
                    (select count(ct.id) from Contract ct where ct.customer = c and ct.deletedAt is null),
                    (select coalesce(sum(ct.monthlyPremium), 0) from Contract ct where ct.customer = c and ct.deletedAt is null),
                    case
                        when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.COMPLETED then (
                            select max(cmc.contractEndDate)
                            from ContractMonthlyClosing cmc
                            where cmc.customer = c
                              and cmc.closingMonth = (
                                  select max(cmc2.closingMonth)
                                  from ContractMonthlyClosing cmc2
                                  where cmc2.customer = c
                              )
                              and cmc.contractStatus = 'COMPLETED'
                        )
                        else null
                    end,
                    case
                        when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.TERMINATED then (
                            select max(cmc.terminatedAt)
                            from ContractMonthlyClosing cmc
                            where cmc.customer = c
                              and cmc.closingMonth = (
                                  select max(cmc2.closingMonth)
                                  from ContractMonthlyClosing cmc2
                                  where cmc2.customer = c
                              )
                              and (cmc.terminatedYn = true or cmc.contractStatus = 'TERMINATED')
                        )
                        else null
                    end,
                    (select max(cs.consultedAt) from Consultation cs where cs.customer = c and cs.deletedAt is null),
                    case
                        when c.customerStatus in (
                            com.linker.relia.customer.domain.CustomerStatus.COMPLETED,
                            com.linker.relia.customer.domain.CustomerStatus.TERMINATED
                        ) then null
                        else (
                            select min(cs.nextScheduledAt) from Consultation cs
                            where cs.customer = c
                              and cs.deletedAt is null
                              and cs.nextScheduledAt is not null
                              and cs.nextScheduledAt >= current_timestamp
                        )
                    end,
                    c.customerGrade,
                    c.customerStatus,
                    org.id,
                    org.organizationCode,
                    org.organizationName
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildWhereClause(accessScope) + """
                order by c.createdAt desc
                """;

        TypedQuery<CustomerListItemResponse> contentQuery =
                entityManager.createQuery(contentJpql, CustomerListItemResponse.class);
        bindAccessScope(contentQuery, accessScope);
        contentQuery.setParameter("customerName", customerName);
        contentQuery.setParameter("organizationCode", organizationCode);
        contentQuery.setParameter("customerStatus", customerStatus);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        String countJpql = """
                select count(c)
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildWhereClause(accessScope);

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        bindAccessScope(countQuery, accessScope);
        countQuery.setParameter("customerName", customerName);
        countQuery.setParameter("organizationCode", organizationCode);
        countQuery.setParameter("customerStatus", customerStatus);

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    @Override
    public Page<CustomerInterestItemResponse> searchInterestCustomers(AccessScope accessScope,
                                                                      String customerName,
                                                                      String organizationCode,
                                                                      String interestReason,
                                                                      Pageable pageable) {
        String fromWhereSql = """
                from customers c
                join users fp on fp.id = c.customer_fp_id
                join organizations org on org.id = fp.organization_id
                where c.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                  and c.interest_yn = true
                  and (:customerName is null or c.customer_name like concat('%', :customerName, '%'))
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:interestReason is null or c.interest_reason = :interestReason)
                """ + buildInterestAccessScopeWhereClause(accessScope);

        String contentSql = """
                select
                    c.id,
                    c.customer_name,
                    c.customer_birth_date,
                    c.customer_phone,
                    c.customer_status,
                    c.interest_reason,
                    (
                        select max(cs.consulted_at)
                        from consultations cs
                        where cs.customer_id = c.id
                          and cs.deleted_at is null
                    ) as last_consulted_at,
                    case
                        when c.interest_reason = 'UNPAID' then (
                            select max(cmc.current_payment_round)
                            from contract_monthly_closing cmc
                            join contracts ct on ct.id = cmc.contract_id
                            where cmc.customer_id = c.id
                              and ct.deleted_at is null
                              and cmc.contract_status = 'MAINTENANCE'
                              and cmc.payment_status = 'UNPAID'
                              and cmc.closing_month = (
                                  select max(cmc2.closing_month)
                                  from contract_monthly_closing cmc2
                                  join contracts ct2 on ct2.id = cmc2.contract_id
                                  where cmc2.contract_id = cmc.contract_id
                                    and ct2.deleted_at is null
                              )
                        )
                        else null
                    end as unpaid_installment_count,
                    case
                        when c.interest_reason = 'RENEWAL_DUE' then (
                            select min(datediff(ct.contract_end_date, current_date))
                            from contracts ct
                            join insurance_products ip on ip.id = ct.insurance_product_id
                            where ct.customer_id = c.id
                              and ct.deleted_at is null
                              and ip.deleted_at is null
                              and ct.contract_status = 'MAINTENANCE'
                              and ip.is_renewable = true
                              and ct.contract_end_date between current_date and date_add(current_date, interval 30 day)
                        )
                        else null
                    end as renewal_d_day,
                    case
                        when c.interest_reason = 'MATURITY_DUE' then (
                            select min(datediff(ct.contract_end_date, current_date))
                            from contracts ct
                            join insurance_products ip on ip.id = ct.insurance_product_id
                            where ct.customer_id = c.id
                              and ct.deleted_at is null
                              and ip.deleted_at is null
                              and ct.contract_status = 'MAINTENANCE'
                              and ip.is_renewable = false
                              and ct.contract_end_date between current_date and date_add(current_date, interval 30 day)
                        )
                        else null
                    end as maturity_d_day,
                    org.id,
                    org.organization_code,
                    org.organization_name
                """ + fromWhereSql + """
                order by c.created_at desc
                """;

        Query contentQuery = entityManager.createNativeQuery(contentSql);
        bindInterestSearchParameters(contentQuery, accessScope, customerName, organizationCode, interestReason);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = contentQuery.getResultList();
        List<CustomerInterestItemResponse> content = rows.stream()
                .map(this::toCustomerInterestItemResponse)
                .toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + fromWhereSql);
        bindInterestSearchParameters(countQuery, accessScope, customerName, organizationCode, interestReason);

        return new PageImpl<>(content, pageable, toLong(countQuery.getSingleResult()));
    }

    @Override
    public boolean existsAccessibleCustomer(AccessScope accessScope, UUID customerId) {
        String existsJpql = """
                select count(c)
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildDetailWhereClause(accessScope);

        TypedQuery<Long> existsQuery = entityManager.createQuery(existsJpql, Long.class);
        bindAccessScope(existsQuery, accessScope);
        existsQuery.setParameter("customerId", customerId);

        return existsQuery.getSingleResult() > 0;
    }

    @Override
    public Optional<CustomerDetailQueryResult> findCustomerDetail(AccessScope accessScope,
                                                                  UUID customerId) {
        String detailJpql = """
                select new com.linker.relia.customer.dto.CustomerDetailQueryResult(
                    c.id,
                    c.customerName,
                    c.customerStatus,
                    c.interestYn,
                    c.customerGrade,
                    c.customerBirthDate,
                    c.customerGender,
                    c.customerPhone,
                    c.customerEmail,
                    case
                        when c.customerAddressDetail is null or c.customerAddressDetail = ''
                            then c.customerAddressRoad
                        else concat(c.customerAddressRoad, ', ', c.customerAddressDetail)
                    end,
                    c.customerJob,
                    c.customerCompanyName,
                    fp.id,
                    fp.userName,
                    org.organizationCode,
                    org.organizationName,
                    (select max(cs.consultedAt)
                        from Consultation cs
                       where cs.customer = c
                         and cs.deletedAt is null),
                    case
                        when c.customerStatus in (
                            com.linker.relia.customer.domain.CustomerStatus.COMPLETED,
                            com.linker.relia.customer.domain.CustomerStatus.TERMINATED
                        ) then null
                        else (
                            select min(cs.nextScheduledAt)
                            from Consultation cs
                           where cs.customer = c
                             and cs.deletedAt is null
                             and cs.nextScheduledAt is not null
                             and cs.nextScheduledAt >= current_timestamp
                        )
                    end
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildDetailWhereClause(accessScope);

        TypedQuery<CustomerDetailQueryResult> detailQuery =
                entityManager.createQuery(detailJpql, CustomerDetailQueryResult.class);
        bindAccessScope(detailQuery, accessScope);
        detailQuery.setParameter("customerId", customerId);

        List<CustomerDetailQueryResult> results = detailQuery.getResultList();
        return results.stream().findFirst();
    }

    private void bindAccessScope(TypedQuery<?> query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId());
        }
    }

    private void bindAccessScope(Query query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId().toString());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId().toString());
        }
    }

    private void bindInterestSearchParameters(Query query,
                                              AccessScope accessScope,
                                              String customerName,
                                              String organizationCode,
                                              String interestReason) {
        query.setParameter("customerName", customerName);
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("interestReason", interestReason);
        bindAccessScope(query, accessScope);
    }

    private String buildSummaryWhereClause(AccessScope accessScope) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and (:organizationCode is null or org.organizationCode = :organizationCode)
                """);

        if (accessScope.isOwnScope()) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (accessScope.isBranchScope()) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }

    private String buildWhereClause(AccessScope accessScope) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and (:customerName is null or c.customerName like concat('%', :customerName, '%'))
                  and (:organizationCode is null or org.organizationCode = :organizationCode)
                  and (:customerStatus is null or c.customerStatus = :customerStatus)
                """);

        if (accessScope.isOwnScope()) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (accessScope.isBranchScope()) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }

    private String buildDetailWhereClause(AccessScope accessScope) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and c.id = :customerId
                """);

        if (accessScope.isOwnScope()) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (accessScope.isBranchScope()) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }

    private String buildInterestAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "\n  and fp.id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "\n  and org.id = :organizationId\n";
        }

        return "\n";
    }

    private CustomerInterestItemResponse toCustomerInterestItemResponse(Object[] row) {
        return CustomerInterestItemResponse.builder()
                .customerId(toUuid(row[0]))
                .customerName((String) row[1])
                .customerBirthDate(toLocalDate(row[2]))
                .customerPhone((String) row[3])
                .customerStatus(toCustomerStatus(row[4]))
                .interestReason(toInterestReason(row[5]))
                .lastConsultedAt(toLocalDateTime(row[6]))
                .unpaidInstallmentCount(toInteger(row[7]))
                .renewalDDay(toInteger(row[8]))
                .maturityDDay(toInteger(row[9]))
                .organizationId(toUuid(row[10]))
                .organizationCode((String) row[11])
                .organizationName((String) row[12])
                .build();
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private UUID toUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        return value == null ? null : LocalDate.parse(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return value == null ? null : LocalDateTime.parse(value.toString().replace(" ", "T"));
    }

    private CustomerStatus toCustomerStatus(Object value) {
        return value == null ? null : CustomerStatus.valueOf(value.toString());
    }

    private InterestReason toInterestReason(Object value) {
        return value == null ? null : InterestReason.valueOf(value.toString());
    }
}
