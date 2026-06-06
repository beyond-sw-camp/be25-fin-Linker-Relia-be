package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.CustomerStatus;
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

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
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
