package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationRenewalDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRenewalDetailRepository
        extends JpaRepository<ConsultationRenewalDetail, UUID> {
    Optional<ConsultationRenewalDetail>
    findByConsultationId(UUID consultationId);
}
