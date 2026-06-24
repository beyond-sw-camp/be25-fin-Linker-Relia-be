package com.linker.relia.customer.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerOwnedContractRequest extends PageQueryRequest {
    private CustomerOwnedContractStatus contractStatus;
}
