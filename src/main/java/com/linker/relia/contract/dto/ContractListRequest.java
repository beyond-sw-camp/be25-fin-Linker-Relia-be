package com.linker.relia.contract.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContractListRequest extends PageQueryRequest {
    private String organizationCode;
    private UUID insuranceCompanyId;
    private ContractListStatus contractStatus;
    private ContractListSort sort = ContractListSort.LATEST_CONTRACT;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "closingMonth는 YYYY-MM 형식이어야 합니다.")
    private String closingMonth;
}
