package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.common.audit.AuditContextHolder;
import com.linker.relia.common.exception.BusinessException;
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
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationAiNoteServiceImpl implements ConsultationAiNoteService {
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationAiNoteRepository consultationAiNoteRepository;
    private final ConsultationAiDraftGenerator consultationAiDraftGenerator;
    private final ConsultationAiDraftNormalizer draftNormalizer;
    private final ConsultationAiResolutionService resolutionService;
    private final ObjectMapper objectMapper;
    private final TransactionOperations transactionOperations;

    @Override
    public void processSttCompleted(UUID sessionId, UUID fpId, String sttRawText) {
        AuditContextHolder.setCurrentAuditor(fpId);
        try {
            SttCompletionContext context = transactionOperations.execute(status -> {
                ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, fpId);
                ConsultationAiNote aiNote = findOrCreateAiNote(session);
                if (aiNote.getDraftStatus() == ConsultationAiNoteStatus.GPT_COMPLETED
                        || aiNote.getDraftStatus() == ConsultationAiNoteStatus.APPLIED) {
                    return new SttCompletionContext(session, aiNote.getId(), false);
                }

                aiNote.completeStt(sttRawText);
                consultationAiNoteRepository.flush();
                return new SttCompletionContext(session, aiNote.getId(), true);
            });
            if (context == null) {
                throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
            }

            if (!context.shouldGenerate()) {
                log.info("Skip duplicate AI draft generation because note is already completed. sessionId={}, status={}",
                        sessionId, consultationAiNoteRepository.findByIdAndDeletedAtIsNull(context.aiNoteId())
                                .map(ConsultationAiNote::getDraftStatus)
                                .orElse(null));
                return;
            }

            try {
                ConsultationAiGenerationResult result = consultationAiDraftGenerator.generate(context.session(), sttRawText);
                ConsultationAiStructuredDraft enrichedDraft =
                        draftNormalizer.enrichStructuredDraft(context.session(), result.getStructuredData());
                ConsultationAiDraftNormalizationResult normalized =
                        draftNormalizer.normalizeWithWarnings(enrichedDraft, sttRawText);
                if (!normalized.warnings().isEmpty()) {
                    log.info("Normalized AI structured draft during STT completion. sessionId={} warnings={}",
                            sessionId, normalized.warnings());
                }
                String structuredData = writeStructuredData(normalized.draft());
                transactionOperations.executeWithoutResult(status -> completeGpt(context.aiNoteId(), result.getSummaryText(), structuredData));
            } catch (Exception e) {
                log.warn("AI consultation draft generation failed. sessionId={}", sessionId, e);
                transactionOperations.executeWithoutResult(status -> markFailed(context.aiNoteId(), e.getMessage()));
            }
        } finally {
            AuditContextHolder.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAiDraftResponse getAiDraft(UUID sessionId, UUID fpId) {
        ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, fpId);
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId)
                .orElse(null);

        if (aiNote == null) {
            return ConsultationAiDraftResponse.builder()
                    .sessionId(sessionId)
                    .consultationType(session.getConsultationType())
                    .draftStatus(ConsultationAiNoteStatus.PENDING)
                    .build();
        }

        session = aiNote.getSttSession();
        ConsultationAiDraftNormalizationResult normalized = draftNormalizer.normalizeStoredStructuredData(
                aiNote.getGptStructuredData(),
                aiNote.getSttRawText()
        );
        ConsultationAiDraftResolutionResult resolved = resolutionService.resolveMappings(session, normalized.draft());

        return ConsultationAiDraftResponse.builder()
                .aiNoteId(aiNote.getId())
                .sessionId(sessionId)
                .consultationType(aiNote.getConsultationType())
                .draftStatus(aiNote.getDraftStatus())
                .sttRawText(aiNote.getSttRawText())
                .summaryText(aiNote.getGptSummaryText())
                .structuredData(resolved.draft())
                .resolutions(resolved.resolution())
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

    private void completeGpt(UUID aiNoteId, String summaryText, String structuredData) {
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findByIdAndDeletedAtIsNull(aiNoteId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));
        aiNote.completeGpt(summaryText, structuredData);
    }

    private void markFailed(UUID aiNoteId, String errorMessage) {
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findByIdAndDeletedAtIsNull(aiNoteId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));
        aiNote.markFailed(errorMessage);
    }

    private String writeStructuredData(ConsultationAiStructuredDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }

    private record SttCompletionContext(
            ConsultationSttSession session,
            UUID aiNoteId,
            boolean shouldGenerate
    ) {
    }
}
