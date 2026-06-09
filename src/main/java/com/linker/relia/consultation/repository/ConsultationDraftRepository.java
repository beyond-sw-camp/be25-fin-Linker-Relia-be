package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationDraftRepository extends JpaRepository<ConsultationDraft, UUID> {

    Optional<ConsultationDraft> findByIdAndFpId (
            UUID draftId,
            UUID fpId
    );

    List<ConsultationDraft> findAllByFpIdOrderByLastSavedAtDesc(UUID fpId);
}
