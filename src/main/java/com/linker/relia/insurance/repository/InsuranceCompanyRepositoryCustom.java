package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.dto.response.InsuranceCompanyListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InsuranceCompanyRepositoryCustom {
    Page<InsuranceCompanyListItemResponse> searchPartnerInsuranceCompanies(String insuranceCompanyName,
                                                                           Pageable pageable);
}
