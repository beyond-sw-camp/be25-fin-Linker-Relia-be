package com.linker.relia.contract.repository;

import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContractRepositoryCustom {
    CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId,
                                                              LocalDate referenceDate,
                                                              LocalDate dueDateLimit);

    List<CustomerOwnedContractResponse> findOwnCustomerContracts(UUID customerId);
}
