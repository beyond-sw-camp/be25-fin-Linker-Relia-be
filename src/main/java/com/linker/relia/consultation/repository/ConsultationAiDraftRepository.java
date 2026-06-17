package com.linker.relia.consultation.repository;

import com.linker.relia.consultation.domain.ConsultationAiDraft;
import com.linker.relia.consultation.domain.ConsultationAiDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationAiDraftRepository extends JpaRepository<ConsultationAiDraft, UUID> {

    Optional<ConsultationAiDraft> findTopByAudioRecord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID audioRecordId);

    List<ConsultationAiDraft> findAllByAudioRecord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID audioRecordId);

    List<ConsultationAiDraft> findAllByDraftStatusAndDeletedAtIsNull(ConsultationAiDraftStatus draftStatus);

    Optional<ConsultationAiDraft> findByIdAndDeletedAtIsNull(UUID id);
}
