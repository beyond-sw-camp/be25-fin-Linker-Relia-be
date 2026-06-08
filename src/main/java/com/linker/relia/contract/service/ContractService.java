package com.linker.relia.contract.service;

import com.linker.relia.contract.dto.ContractSummaryRequest;
import com.linker.relia.contract.dto.ContractSummaryResponse;
import com.linker.relia.security.principal.PrincipalDetails;

public interface ContractService {
    ContractSummaryResponse getContractSummary(PrincipalDetails principalDetails,
                                               ContractSummaryRequest request);
}
