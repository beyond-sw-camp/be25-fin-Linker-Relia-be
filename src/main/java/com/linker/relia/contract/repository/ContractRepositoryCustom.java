package com.linker.relia.contract.repository;

import com.linker.relia.customer.dto.CustomerContractSummaryResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface ContractRepositoryCustom {
    CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId,
                                                              LocalDate referenceDate,
                                                              LocalDate dueDateLimit);
}
