package com.linker.relia.insurance.service;

import com.linker.relia.insurance.dto.InsuranceCategoryResponse;
import com.linker.relia.insurance.dto.InsuranceCompanyResponse;
import com.linker.relia.insurance.dto.InsuranceProductListRequest;
import com.linker.relia.insurance.dto.InsuranceProductResponse;
import com.linker.relia.insurance.repository.InsuranceCategoryRepository;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InsuranceServiceImpl implements InsuranceService {
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final InsuranceCategoryRepository insuranceCategoryRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCompanyResponse> getInsuranceCompanies() {
        return insuranceCompanyRepository
                .findAllByInsuranceCompanyStatusAndDeletedAtIsNullOrderByInsuranceCompanyNameAsc(ACTIVE_STATUS)
                .stream()
                .map(InsuranceCompanyResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceCategoryResponse> getInsuranceCategories() {
        return insuranceCategoryRepository
                .findAllByInsuranceCategoryStatusAndDeletedAtIsNullOrderByInsuranceCategoryNameAsc(ACTIVE_STATUS)
                .stream()
                .map(InsuranceCategoryResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsuranceProductResponse> getInsuranceProducts(InsuranceProductListRequest request) {
        return insuranceProductRepository
                .searchActiveInsuranceProducts(
                        request.getInsuranceCompanyId(),
                        request.getInsuranceCategoryId(),
                        ACTIVE_STATUS
                )
                .stream()
                .map(InsuranceProductResponse::from)
                .toList();
    }
}
