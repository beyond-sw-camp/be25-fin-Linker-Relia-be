package com.linker.relia.contract.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListSort;
import com.linker.relia.contract.dto.ContractListStatus;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
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

    Page<ContractListItemResponse> searchHoldingContracts(AccessScope accessScope,
                                                          String organizationCode,
                                                          UUID insuranceCompanyId,
                                                          String closingMonth,
                                                          ContractListStatus contractStatus,
                                                          ContractListSort sort,
                                                          LocalDate referenceDate,
                                                          LocalDate dueDateLimit,
                                                          Pageable pageable);

    List<CustomerOwnedContractResponse> findOwnCustomerContracts(UUID customerId);
}
