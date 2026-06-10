package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationNewCoverageNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationNewCoverageNeedRepository extends JpaRepository<ConsultationNewCoverageNeed, UUID> {
}
