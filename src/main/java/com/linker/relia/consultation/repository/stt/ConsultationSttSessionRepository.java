package com.linker.relia.consultation.repository.stt;

import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationSttSessionRepository extends JpaRepository<ConsultationSttSession, UUID> {

    Optional<ConsultationSttSession> findByIdAndDeletedAtIsNull(UUID id);

    List<ConsultationSttSession> findAllByFp_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID fpId);
}
