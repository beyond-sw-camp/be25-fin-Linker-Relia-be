package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.request.InsuranceCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyPageResponse;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsuranceCompanyServiceImpl implements InsuranceCompanyService {
    private final InsuranceCompanyRepository insuranceCompanyRepository;

    @Override
    @Transactional(readOnly = true)
    public InsuranceCompanyPageResponse getPartnerInsuranceCompanies(InsuranceCompanyListRequest request) {
        return InsuranceCompanyPageResponse.from(insuranceCompanyRepository.searchPartnerInsuranceCompanies(
                request.normalizedInsuranceCompanyName(),
                request.toPageable()
        ));
    }
}
