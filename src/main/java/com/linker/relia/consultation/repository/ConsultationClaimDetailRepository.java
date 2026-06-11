package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationClaimDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationClaimDetailRepository
        extends JpaRepository<ConsultationClaimDetail, UUID> {
    Optional<ConsultationClaimDetail> findByConsultationId(UUID consultationId);
}
