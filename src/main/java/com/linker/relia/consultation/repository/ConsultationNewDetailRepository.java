package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationNewDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationNewDetailRepository
        extends JpaRepository<ConsultationNewDetail, UUID> {

    Optional<ConsultationNewDetail> findByConsultationId(UUID consultationId);
}
