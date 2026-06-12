package com.linker.relia.contract.repository;

import com.linker.relia.contract.domain.Contract;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.insurance.domain.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID>, ContractRepositoryCustom {
    @Query(value = """
            select coalesce(max(cast(substring(contract_code, 4) as unsigned)), 0)
            from contracts
            where contract_code regexp '^CTR[0-9]+$'
            """, nativeQuery = true)
    long findMaxContractCodeSequence();

    boolean existsByCustomerAndInsuranceProductAndContractStatusInAndDeletedAtIsNull(
            Customer customer,
            InsuranceProduct insuranceProduct,
            Collection<String> contractStatuses
    );
}
