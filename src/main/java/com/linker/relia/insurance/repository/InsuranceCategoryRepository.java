package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsuranceCategoryRepository extends JpaRepository<InsuranceCategory, UUID> {
    List<InsuranceCategory> findAllByInsuranceCategoryStatusAndDeletedAtIsNullOrderByInsuranceCategoryNameAsc(
            String insuranceCategoryStatus
    );
}
