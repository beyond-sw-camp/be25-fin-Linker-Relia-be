package com.linker.relia.contract.repository;

import com.linker.relia.common.access.AccessScope;
import com.linker.relia.contract.dto.ContractDetailQueryResult;
import com.linker.relia.contract.dto.ContractListItemResponse;
import com.linker.relia.contract.dto.ContractListSort;
import com.linker.relia.contract.dto.ContractListStatus;
import com.linker.relia.contract.dto.ContractMonthlyTrendResponse;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.contract.dto.InsuranceCompanyContractStatusResponse;
import com.linker.relia.customer.dto.CustomerContractSummaryResponse;
import com.linker.relia.customer.dto.CustomerOwnedContractResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepositoryCustom {
    CustomerContractSummaryResponse summarizeCustomerContracts(UUID customerId);


    ContractSummaryResponse summarizeHoldingContracts(AccessScope accessScope,
                                                      String organizationCode,
                                                      UUID insuranceCompanyId,
                                                      LocalDate referenceDate,
                                                      LocalDate dueDateLimit);

    Page<ContractListItemResponse> searchHoldingContracts(AccessScope accessScope,
                                                          String organizationCode,
                                                          UUID insuranceCompanyId,
                                                          ContractListStatus contractStatus,
                                                          ContractListSort sort,
                                                          LocalDate referenceDate,
                                                          LocalDate dueDateLimit,
                                                          Pageable pageable);

    List<InsuranceCompanyContractStatusResponse> summarizeInsuranceCompanyContractStatuses(AccessScope accessScope,
                                                                                           String organizationCode,
                                                                                           UUID insuranceCompanyId);

    List<ContractMonthlyTrendResponse> summarizeMonthlyContractTrend(AccessScope accessScope,
                                                                     String organizationCode,
                                                                     UUID insuranceCompanyId,
                                                                     LocalDate startDate,
                                                                     LocalDate endDate);

    Optional<ContractDetailQueryResult> findContractDetail(AccessScope accessScope,
                                                           UUID contractId);

    List<CustomerOwnedContractResponse> findOwnCustomerContracts(UUID customerId);
}
