package com.linker.relia.organization.dto;

import com.linker.relia.common.dto.request.PageQueryRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FpListRequest extends PageQueryRequest {
    private String keyword;
    private UUID organizationId;
    private String closingMonth;
}
