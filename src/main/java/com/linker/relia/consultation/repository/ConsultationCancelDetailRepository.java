package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationCancelDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsultationCancelDetailRepository extends JpaRepository<ConsultationCancelDetail, UUID> {
}
