package com.linker.relia.insurance.service;

import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.insurance.domain.InsuranceCompany;
import com.linker.relia.insurance.dto.request.InsuranceCompanyCreateRequest;
import com.linker.relia.insurance.dto.request.InsuranceCompanyUpdateRequest;
import com.linker.relia.insurance.dto.request.InsuranceManagementCompanyListRequest;
import com.linker.relia.insurance.dto.response.InsuranceCompanyCreateResponse;
import com.linker.relia.insurance.dto.response.InsuranceCompanyDetailResponse;
import com.linker.relia.insurance.dto.response.InsuranceManagementCompanyListItemResponse;
import com.linker.relia.insurance.exception.InsuranceErrorCode;
import com.linker.relia.insurance.repository.InsuranceCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InsuranceManagementServiceImpl implements InsuranceManagementService {
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INSURANCE_COMPANY_CODE_PREFIX = "LC";

    private final InsuranceCompanyRepository insuranceCompanyRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsuranceManagementCompanyListItemResponse> getInsuranceCompanies(
            InsuranceManagementCompanyListRequest request
    ) {
        return PageResponse.from(
                insuranceCompanyRepository.searchManagementInsuranceCompanies(
                                request.normalizedInsuranceCompanyName(),
                                request.toPageable()
                        )
                        .map(InsuranceManagementCompanyListItemResponse::from)
        );
    }

    @Override
    @Transactional
    public InsuranceCompanyCreateResponse createInsuranceCompany(InsuranceCompanyCreateRequest request) {
        String insuranceCompanyCode = generateInsuranceCompanyCode();

        if (insuranceCompanyRepository.existsByInsuranceCompanyCode(insuranceCompanyCode)) {
            throw new BusinessException(InsuranceErrorCode.DUPLICATE_INSURANCE_COMPANY_CODE);
        }

        InsuranceCompany insuranceCompany = InsuranceCompany.builder()
                .id(UUID.randomUUID())
                .insuranceCompanyCode(insuranceCompanyCode)
                .insuranceCompanyName(request.getInsuranceCompanyName().trim())
                .insuranceCompanyStatus(ACTIVE_STATUS)
                .insuranceCompanyPhone(request.getInsuranceCompanyPhone().trim())
                .deletedAt(null)
                .build();

        InsuranceCompany savedInsuranceCompany = insuranceCompanyRepository.save(insuranceCompany);

        return InsuranceCompanyCreateResponse.builder()
                .insuranceCompanyId(savedInsuranceCompany.getId())
                .insuranceCompanyCode(savedInsuranceCompany.getInsuranceCompanyCode())
                .insuranceCompanyName(savedInsuranceCompany.getInsuranceCompanyName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InsuranceCompanyDetailResponse getInsuranceCompanyDetail(UUID insuranceCompanyId) {
        InsuranceCompany insuranceCompany = insuranceCompanyRepository.findById(insuranceCompanyId)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_COMPANY_NOT_FOUND));

        return InsuranceCompanyDetailResponse.from(insuranceCompany);
    }

    @Override
    @Transactional
    public InsuranceCompanyDetailResponse updateInsuranceCompany(
            UUID insuranceCompanyId,
            InsuranceCompanyUpdateRequest request
    ) {
        InsuranceCompany insuranceCompany = insuranceCompanyRepository.findById(insuranceCompanyId)
                .orElseThrow(() -> new BusinessException(InsuranceErrorCode.INSURANCE_COMPANY_NOT_FOUND));

        insuranceCompany.update(
                request.getInsuranceCompanyName().trim(),
                request.getInsuranceCompanyPhone().trim(),
                request.getInsuranceCompanyStatus().trim()
        );

        return InsuranceCompanyDetailResponse.from(insuranceCompany);
    }

    private String generateInsuranceCompanyCode() {
        long nextSequence = insuranceCompanyRepository.findMaxInsuranceCompanyCodeSequence() + 1;
        return INSURANCE_COMPANY_CODE_PREFIX + String.format("%03d", nextSequence);
    }
}
