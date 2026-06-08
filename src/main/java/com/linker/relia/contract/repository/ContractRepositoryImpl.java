package com.linker.relia.contract.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Repository
public class ContractRepositoryImpl implements ContractRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId,
                                                                     LocalDate referenceDate,
                                                                     LocalDate dueDateLimit) {
        String jpql = """
                select new com.linker.relia.customer.dto.CustomerContractSummaryResponse(
                    count(ct),
                    coalesce(sum(ct.monthlyPremium), 0),
                    coalesce(sum(case when ct.contractStatus = 'MAINTENANCE' then 1L else 0L end), 0L),
                    coalesce(sum(case
                        when ct.contractStatus = 'MAINTENANCE'
                         and ct.contractEndDate between :referenceDate and :dueDateLimit then 1L
                        else 0L
                    end), 0L)
                )
                from Contract ct
                where ct.customer.id = :customerId
                  and ct.deletedAt is null
                """;

        TypedQuery<CustomerContractSummaryResponse> query =
                entityManager.createQuery(jpql, CustomerContractSummaryResponse.class);
        query.setParameter("customerId", customerId);
        query.setParameter("referenceDate", referenceDate);
        query.setParameter("dueDateLimit", dueDateLimit);
        return query.getSingleResult();
    }

    @Override
    public ContractSummaryResponse summarizeHoldingContracts(AccessScope accessScope,
                                                             String organizationCode,
                                                             UUID insuranceCompanyId,
                                                             String closingMonth,
                                                             LocalDate referenceDate,
                                                             LocalDate dueDateLimit) {
        String sql = """
                select
                    count(*) as total_contract_count,
                    coalesce(sum(case
                        when cmc.contract_status = 'MAINTENANCE'
                         and cmc.payment_status = 'PAID' then 1 else 0
                    end), 0) as normal_payment_count,
                    coalesce(sum(case
                        when cmc.contract_status = 'MAINTENANCE'
                         and cmc.payment_status = 'UNPAID' then 1 else 0
                    end), 0) as unpaid_count,
                    coalesce(sum(case
                        when cmc.contract_status = 'LAPSED'
                          or cmc.lapse_yn = true then 1 else 0
                    end), 0) as lapse_expected_count,
                    coalesce(sum(case
                        when cmc.contract_status = 'MAINTENANCE'
                         and cmc.contract_end_date between :referenceDate and :dueDateLimit then 1 else 0
                    end), 0) as expiring_soon_count
                from contract_monthly_closing cmc
                join users fp on fp.id = cmc.fp_id
                join organizations org on org.id = fp.organization_id
                join contracts ct on ct.id = cmc.contract_id
                join insurance_products ip on ip.id = ct.insurance_product_id
                where cmc.closing_month = :closingMonth
                  and cmc.contract_status in ('MAINTENANCE', 'LAPSED')
                  and (:organizationCode is null or org.organization_code = :organizationCode)
                  and (:insuranceCompanyId is null or ip.insurance_company_id = :insuranceCompanyId)
                  and ct.deleted_at is null
                  and fp.deleted_at is null
                  and org.deleted_at is null
                """ + buildAccessScopeWhereClause(accessScope);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("closingMonth", closingMonth);
        query.setParameter("referenceDate", referenceDate);
        query.setParameter("dueDateLimit", dueDateLimit);
        query.setParameter("organizationCode", organizationCode);
        query.setParameter("insuranceCompanyId", insuranceCompanyId == null ? null : insuranceCompanyId.toString());
        bindAccessScope(query, accessScope);

        Object[] row = (Object[]) query.getSingleResult();
        if (row == null) {
            return ContractSummaryResponse.empty();
        }

        return ContractSummaryResponse.builder()
                .totalContractCount(toLong(row[0]))
                .normalPaymentCount(toLong(row[1]))
                .unpaidCount(toLong(row[2]))
                .lapseExpectedCount(toLong(row[3]))
                .expiringSoonCount(toLong(row[4]))
                .build();
    }

    private String buildAccessScopeWhereClause(AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            return "\n  and cmc.fp_id = :userId\n";
        }

        if (accessScope.isBranchScope()) {
            return "\n  and org.id = :organizationId\n";
        }

        return "\n";
    }

    private void bindAccessScope(Query query, AccessScope accessScope) {
        if (accessScope.isOwnScope()) {
            query.setParameter("userId", accessScope.userId().toString());
        } else if (accessScope.isBranchScope()) {
            query.setParameter("organizationId", accessScope.organizationId().toString());
        }
    }

    private long toLong(Object value) {
        return ((Number) Objects.requireNonNull(value)).longValue();
    }
}
