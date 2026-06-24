package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.DiseaseCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiseaseCodeRepository extends JpaRepository<DiseaseCode, UUID> {
    boolean existsByDiseaseCodeAndDeletedAtIsNull(String diseaseCode);

    Optional<DiseaseCode> findByDiseaseNameAndDeletedAtIsNull(String diseaseName);

    List<DiseaseCode> findTop10ByDiseaseNameContainingAndDeletedAtIsNullOrderByDiseaseNameAsc(String diseaseName);
}
