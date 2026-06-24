package com.linker.relia.consultation.repository.stt;

import com.linker.relia.consultation.domain.stt.ConsultationAudioRecord;
import com.linker.relia.consultation.domain.stt.ConsultationAudioUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationAudioRecordRepository extends JpaRepository<ConsultationAudioRecord, UUID> {

    Optional<ConsultationAudioRecord> findByIdAndDeletedAtIsNull(UUID id);

    List<ConsultationAudioRecord> findAllByFp_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID fpId);

    List<ConsultationAudioRecord> findAllByUploadStatusAndDeletedAtIsNull(ConsultationAudioUploadStatus uploadStatus);
}
