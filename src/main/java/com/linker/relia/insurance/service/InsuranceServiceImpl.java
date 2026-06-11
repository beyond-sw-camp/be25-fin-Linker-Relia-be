package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.InsuranceCompanyResponse;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final InsuranceCompanyRepository insuranceCompanyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCompanyResponse> getInsuranceCompanies() {
        return insuranceCompanyRepository
                .findAllByInsuranceCompanyStatusAndDeletedAtIsNullOrderByInsuranceCompanyNameAsc(ACTIVE_STATUS)
                .stream()
                .map(InsuranceCompanyResponse::from)
                .toList();
    }
}
