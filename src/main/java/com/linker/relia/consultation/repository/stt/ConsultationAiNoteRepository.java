package com.linker.relia.consultation.repository.stt;

import com.linker.relia.consultation.domain.stt.ConsultationAiNote;
import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationAiNoteRepository extends JpaRepository<ConsultationAiNote, UUID> {

    Optional<ConsultationAiNote> findTopByAudioRecord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID audioRecordId);

    Optional<ConsultationAiNote> findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID sttSessionId);

    List<ConsultationAiNote> findAllByAudioRecord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID audioRecordId);

    List<ConsultationAiNote> findAllByDraftStatusAndDeletedAtIsNull(ConsultationAiNoteStatus draftStatus);

    Optional<ConsultationAiNote> findByIdAndDeletedAtIsNull(UUID id);
}
