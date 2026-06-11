package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.InsuranceCompanyResponse;

import java.util.List;

public interface InsuranceService {
    List<InsuranceCompanyResponse> getInsuranceCompanies();
}
