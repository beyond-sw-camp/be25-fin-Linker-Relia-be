package com.linker.relia.insurance.repository;

import com.linker.relia.insurance.domain.InsuranceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsuranceCategoryRepository extends JpaRepository<InsuranceCategory, UUID> {
    List<InsuranceCategory> findAllByInsuranceCategoryStatusAndDeletedAtIsNullOrderByInsuranceCategoryNameAsc(
            String insuranceCategoryStatus
    );

    List<InsuranceCategory> findAllByOrderByInsuranceCategoryNameAsc();

    boolean existsByInsuranceCategoryCode(String insuranceCategoryCode);

    boolean existsByInsuranceCategoryName(String insuranceCategoryName);

    boolean existsByInsuranceCategoryNameAndIdNot(String insuranceCategoryName, UUID id);

    Optional<InsuranceCategory> findByIdAndInsuranceCategoryStatusAndDeletedAtIsNull(
            UUID id,
            String insuranceCategoryStatus
    );

    @org.springframework.data.jpa.repository.Query("""
            select coalesce(max(cast(substring(ic.insuranceCategoryCode, 4) as integer)), 0)
            from InsuranceCategory ic
            where ic.insuranceCategoryCode like 'CAT%'
            """)
    long findMaxInsuranceCategoryCodeSequence();
}
