package com.linker.relia.insurance.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InsuranceProductListRequest {
    private UUID insuranceCompanyId;
    private UUID insuranceCategoryId;
}
