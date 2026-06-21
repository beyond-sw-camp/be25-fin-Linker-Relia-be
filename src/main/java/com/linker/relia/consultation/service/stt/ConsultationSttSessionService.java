package com.linker.relia.consultation.service.stt;

import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionCompleteRequest;
import com.linker.relia.consultation.dto.request.ConsultationSttSessionStartRequest;
import com.linker.relia.consultation.dto.response.ConsultationSttSessionResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ConsultationSttSessionService {
    ConsultationSttSessionResponse startSession(UUID fpId, ConsultationSttSessionStartRequest request);

    ConsultationSttSessionResponse completeSession(UUID sessionId, UUID fpId, ConsultationSttSessionCompleteRequest request);

    ConsultationSttSessionResponse getSession(UUID sessionId, UUID fpId);

    ConsultationSttSession getOwnedSession(UUID sessionId, UUID fpId);

    void updatePartialText(UUID sessionId, UUID fpId, String partialText);

    void completeSession(UUID sessionId, UUID fpId, String finalText, LocalDateTime endedAt);

    void failSession(UUID sessionId, UUID fpId, String errorMessage, LocalDateTime endedAt);
}
