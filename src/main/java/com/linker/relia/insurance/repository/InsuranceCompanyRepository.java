package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompany, UUID>, InsuranceCompanyRepositoryCustom {
    List<InsuranceCompany> findAllByInsuranceCompanyStatusAndDeletedAtIsNullOrderByInsuranceCompanyNameAsc(
            String insuranceCompanyStatus
    );
}
