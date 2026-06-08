package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.CustomerStatus;
import com.linker.relia.customer.dto.CustomerDetailQueryResult;
import com.linker.relia.customer.dto.CustomerListItemResponse;
import com.linker.relia.customer.dto.CustomerListSummaryResponse;
import com.linker.relia.customer.policy.CustomerAccessScopeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerRepositoryImpl implements CustomerRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerListSummaryResponse summarizeCustomers(CustomerAccessScopeType scopeType,
                                                          UUID userId,
                                                          UUID organizationId,
                                                          String organizationCode) {
        String summaryJpql = """
                select new com.linker.relia.customer.dto.CustomerListSummaryResponse(
                    count(c),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.CONTRACTED then 1L else 0L end), 0L),
                    coalesce(sum(case when c.customerStatus = com.linker.relia.customer.domain.CustomerStatus.PROSPECT then 1L else 0L end), 0L),
                    coalesce(sum(case when c.interestYn = true then 1L else 0L end), 0L)
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildSummaryWhereClause(scopeType);

        TypedQuery<CustomerListSummaryResponse> summaryQuery =
                entityManager.createQuery(summaryJpql, CustomerListSummaryResponse.class);

        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            summaryQuery.setParameter("userId", userId);
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            summaryQuery.setParameter("organizationId", organizationId);
        }
        summaryQuery.setParameter("organizationCode", organizationCode);

        return summaryQuery.getSingleResult();
    }

    @Override
    public Page<CustomerListItemResponse> searchCustomers(CustomerAccessScopeType scopeType,
                                                          UUID userId,
                                                          UUID organizationId,
                                                          String customerName,
                                                          String organizationCode,
                                                          CustomerStatus customerStatus,
                                                          Boolean interestYn,
                                                          Pageable pageable) {
        String contentJpql = """
                select new com.linker.relia.customer.dto.CustomerListItemResponse(
                    c.id,
                    c.customerName,
                    c.customerBirthDate,
                    c.customerPhone,
                    (select count(ct.id) from Contract ct where ct.customer = c and ct.deletedAt is null),
                    (select coalesce(sum(ct.monthlyPremium), 0) from Contract ct where ct.customer = c and ct.deletedAt is null),
                    (select max(cs.consultedAt) from Consultation cs where cs.customer = c and cs.deletedAt is null),
                    (select min(cs.nextScheduledAt) from Consultation cs
                        where cs.customer = c
                          and cs.deletedAt is null
                          and cs.nextScheduledAt is not null
                          and cs.nextScheduledAt >= current_timestamp),
                    c.customerGrade,
                    c.customerStatus,
                    c.interestYn,
                    c.interestReason,
                    org.id,
                    org.organizationCode,
                    org.organizationName
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildWhereClause(scopeType) + """
                order by c.createdAt desc
                """;

        TypedQuery<CustomerListItemResponse> contentQuery =
                entityManager.createQuery(contentJpql, CustomerListItemResponse.class);
        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            contentQuery.setParameter("userId", userId);
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            contentQuery.setParameter("organizationId", organizationId);
        }
        contentQuery.setParameter("customerName", customerName);
        contentQuery.setParameter("organizationCode", organizationCode);
        contentQuery.setParameter("customerStatus", customerStatus);
        contentQuery.setParameter("interestYn", interestYn);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        String countJpql = """
                select count(c)
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildWhereClause(scopeType);

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            countQuery.setParameter("userId", userId);
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            countQuery.setParameter("organizationId", organizationId);
        }
        countQuery.setParameter("customerName", customerName);
        countQuery.setParameter("organizationCode", organizationCode);
        countQuery.setParameter("customerStatus", customerStatus);
        countQuery.setParameter("interestYn", interestYn);

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    @Override
    public Optional<CustomerDetailQueryResult> findCustomerDetail(CustomerAccessScopeType scopeType,
                                                                  UUID userId,
                                                                  UUID organizationId,
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
                    (select min(cs.nextScheduledAt)
                        from Consultation cs
                       where cs.customer = c
                         and cs.deletedAt is null
                         and cs.nextScheduledAt is not null
                         and cs.nextScheduledAt >= current_timestamp)
                )
                from Customer c
                join c.customerFp fp
                join fp.organization org
                """ + buildDetailWhereClause(scopeType);

        TypedQuery<CustomerDetailQueryResult> detailQuery =
                entityManager.createQuery(detailJpql, CustomerDetailQueryResult.class);
        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            detailQuery.setParameter("userId", userId);
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            detailQuery.setParameter("organizationId", organizationId);
        }
        detailQuery.setParameter("customerId", customerId);

        List<CustomerDetailQueryResult> results = detailQuery.getResultList();
        return results.stream().findFirst();
    }

    private String buildSummaryWhereClause(CustomerAccessScopeType scopeType) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and (:organizationCode is null or org.organizationCode = :organizationCode)
                """);

        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }

    private String buildWhereClause(CustomerAccessScopeType scopeType) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and (:customerName is null or c.customerName like concat('%', :customerName, '%'))
                  and (:organizationCode is null or org.organizationCode = :organizationCode)
                  and (:customerStatus is null or c.customerStatus = :customerStatus)
                  and (:interestYn is null or c.interestYn = :interestYn)
                """);

        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }

    private String buildDetailWhereClause(CustomerAccessScopeType scopeType) {
        StringBuilder whereClause = new StringBuilder("""
                where c.deletedAt is null
                  and fp.deletedAt is null
                  and org.deletedAt is null
                  and c.id = :customerId
                """);

        if (scopeType == CustomerAccessScopeType.OWN_CUSTOMERS) {
            whereClause.append("\n  and fp.id = :userId");
        } else if (scopeType == CustomerAccessScopeType.BRANCH_CUSTOMERS) {
            whereClause.append("\n  and org.id = :organizationId");
        }

        whereClause.append("\n");
        return whereClause.toString();
    }
}
