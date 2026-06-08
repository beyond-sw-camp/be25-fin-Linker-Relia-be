package com.linker.relia.contract.repository;

import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
    public List<CustomerOwnedContractResponse> findOwnCustomerContracts(UUID customerId) {
        String jpql = """
                select new com.linker.relia.customer.dto.CustomerOwnedContractResponse(
                    ct.id,
                    ic.insuranceCompanyName,
                    ip.insuranceProductName,
                    ct.monthlyPremium,
                    ct.contractStartDate,
                    ct.contractStatus
                )
                from Contract ct
                join ct.insuranceProduct ip
                join ip.insuranceCompany ic
                where ct.customer.id = :customerId
                  and ct.deletedAt is null
                  and ip.deletedAt is null
                  and ic.deletedAt is null
                order by ct.contractStartDate desc, ct.createdAt desc
                """;

        TypedQuery<CustomerOwnedContractResponse> query =
                entityManager.createQuery(jpql, CustomerOwnedContractResponse.class);
        query.setParameter("customerId", customerId);
        return query.getResultList();
    }
}
