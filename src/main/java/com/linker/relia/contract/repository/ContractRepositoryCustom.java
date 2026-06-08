package com.linker.relia.contract.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface ContractRepositoryCustom {
    CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId,
                                                              LocalDate referenceDate,
                                                              LocalDate dueDateLimit);

    ContractSummaryResponse summarizeHoldingContracts(AccessScope accessScope,
                                                      String organizationCode,
                                                      UUID insuranceCompanyId,
                                                      String closingMonth,
                                                      LocalDate referenceDate,
                                                      LocalDate dueDateLimit);
}
