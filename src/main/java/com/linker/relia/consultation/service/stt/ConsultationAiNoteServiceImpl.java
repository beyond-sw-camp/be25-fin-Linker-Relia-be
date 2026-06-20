package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.ConsultationType;
import com.linker.relia.consultation.domain.stt.ConsultationAiNote;
import com.linker.relia.consultation.domain.stt.ConsultationAiNoteStatus;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationAiDraftResponse;
import com.linker.relia.consultation.dto.response.ConsultationAiGenerationResult;
import com.linker.relia.consultation.dto.response.ConsultationAiStructuredDraft;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.consultation.repository.stt.ConsultationAiNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAiNoteServiceImpl implements ConsultationAiNoteService {
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationAiNoteRepository consultationAiNoteRepository;
    private final ConsultationAiDraftGenerator consultationAiDraftGenerator;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processSttCompleted(UUID sessionId, UUID fpId, String sttRawText) {
        ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, fpId);
        ConsultationAiNote aiNote = findOrCreateAiNote(session);
        aiNote.completeStt(sttRawText);

        try {
            ConsultationAiGenerationResult result = consultationAiDraftGenerator.generate(session, sttRawText);
            ConsultationAiStructuredDraft structuredDraft =
                    enrichStructuredDraft(session, result.getStructuredData());
            aiNote.completeGpt(
                    result.getSummaryText(),
                    objectMapper.writeValueAsString(structuredDraft)
            );
        } catch (Exception e) {
            log.warn("AI 상담 초안 생성에 실패했습니다. sessionId={}", sessionId, e);
            aiNote.markFailed(e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAiDraftResponse getAiDraft(UUID sessionId, UUID fpId) {
        consultationSttSessionService.getOwnedSession(sessionId, fpId);
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));

        return ConsultationAiDraftResponse.builder()
                .aiNoteId(aiNote.getId())
                .sessionId(sessionId)
                .consultationType(aiNote.getConsultationType())
                .draftStatus(aiNote.getDraftStatus())
                .sttRawText(aiNote.getSttRawText())
                .summaryText(aiNote.getGptSummaryText())
                .structuredData(parseStructuredData(aiNote.getGptStructuredData()))
                .errorMessage(aiNote.getErrorMessage())
                .build();
    }

    private ConsultationAiNote findOrCreateAiNote(ConsultationSttSession session) {
        return consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(session.getId())
                .orElseGet(() -> consultationAiNoteRepository.save(
                        ConsultationAiNote.builder()
                                .audioRecord(null)
                                .sttSession(session)
                                .consultationType(session.getConsultationType())
                                .draftStatus(ConsultationAiNoteStatus.PENDING)
                                .build()
                ));
    }

    private ConsultationAiStructuredDraft enrichStructuredDraft(
            ConsultationSttSession session,
            ConsultationAiStructuredDraft structuredDraft
    ) {
        ConsultationAiStructuredDraft draft =
                structuredDraft != null ? structuredDraft : new ConsultationAiStructuredDraft();

        draft.setConsultationType(session.getConsultationType());
        if (draft.getConsultedAt() == null) {
            draft.setConsultedAt(session.getStartedAt());
        }
        if (draft.getCustomerId() == null && session.getCustomer() != null) {
            draft.setCustomerId(session.getCustomer().getId());
        }
        if (session.getConsultationType() != ConsultationType.NEW_CONTRACT) {
            draft.setCustomerInfo(null);
        }

        return draft;
    }

    private ConsultationAiStructuredDraft parseStructuredData(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(rawJson, ConsultationAiStructuredDraft.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }
}
