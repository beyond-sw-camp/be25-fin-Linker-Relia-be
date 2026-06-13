package com.linker.relia.customer.repository;

import com.linker.relia.customer.domain.DiseaseCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiseaseCodeRepository extends JpaRepository<DiseaseCode, UUID> {
    boolean existsByDiseaseCodeAndDeletedAtIsNull(String diseaseCode);
}
