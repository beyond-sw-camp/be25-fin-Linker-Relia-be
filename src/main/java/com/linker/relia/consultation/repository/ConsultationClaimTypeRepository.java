package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationClaimType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationClaimTypeRepository extends JpaRepository<ConsultationClaimType, UUID> {
}
