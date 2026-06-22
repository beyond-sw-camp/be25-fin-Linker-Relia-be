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
import com.linker.relia.consultation.dto.response.ConsultationAiNoteApplyResponse;
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
    private final ConsultationAiDraftNormalizer draftNormalizer;
    private final ConsultationAiResolutionService resolutionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processSttCompleted(UUID sessionId, UUID fpId, String sttRawText) {
        AuditContextHolder.setCurrentAuditor(fpId);
        try {
            ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, fpId);
            ConsultationAiNote aiNote = findOrCreateAiNote(session);
            aiNote.completeStt(sttRawText);

            try {
                ConsultationAiGenerationResult result = consultationAiDraftGenerator.generate(session, sttRawText);
                ConsultationAiStructuredDraft enrichedDraft =
                        draftNormalizer.enrichStructuredDraft(session, result.getStructuredData());
                ConsultationAiDraftNormalizationResult normalized =
                        draftNormalizer.normalizeWithWarnings(enrichedDraft, sttRawText);
                if (!normalized.warnings().isEmpty()) {
                    log.info("Normalized AI structured draft during STT completion. sessionId={} warnings={}",
                            sessionId, normalized.warnings());
                }
                aiNote.completeGpt(
                        result.getSummaryText(),
                        writeStructuredData(normalized.draft())
                );
            } catch (Exception e) {
                log.warn("AI consultation draft generation failed. sessionId={}", sessionId, e);
                aiNote.markFailed(e.getMessage());
            }
        } finally {
            AuditContextHolder.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAiDraftResponse getAiDraft(UUID sessionId, UUID fpId) {
        consultationSttSessionService.getOwnedSession(sessionId, fpId);
        ConsultationAiNote aiNote = consultationAiNoteRepository
                .findTopBySttSession_IdAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));

        ConsultationSttSession session = aiNote.getSttSession();
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

    @Override
    @Transactional
    public ConsultationAiNoteApplyResponse applyAiDraft(UUID aiNoteId, UUID fpId) {
        AuditContextHolder.setCurrentAuditor(fpId);
        try {
            ConsultationAiNote aiNote = consultationAiNoteRepository
                    .findByIdAndDeletedAtIsNull(aiNoteId)
                    .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_FOUND));

            ConsultationSttSession session = aiNote.getSttSession();
            if (session == null) {
                throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
            }

            consultationSttSessionService.getOwnedSession(session.getId(), fpId);
            validateApplicableStatus(aiNote.getDraftStatus());

            aiNote.markApplied();
            ConsultationAiDraftNormalizationResult normalized = draftNormalizer.normalizeStoredStructuredData(
                    aiNote.getGptStructuredData(),
                    aiNote.getSttRawText()
            );
            ConsultationAiDraftResolutionResult resolved = resolutionService.resolveMappings(session, normalized.draft());

            if (resolved.draft() != null) {
                aiNote.completeGpt(
                        aiNote.getGptSummaryText(),
                        writeStructuredData(resolved.draft())
                );
                aiNote.markApplied();
            }

            if (!normalized.warnings().isEmpty()) {
                log.info("Applied AI draft with warnings. aiNoteId={} warnings={}", aiNoteId, normalized.warnings());
            }
            consultationAiNoteRepository.flush();

            return ConsultationAiNoteApplyResponse.builder()
                    .aiNoteId(aiNote.getId())
                    .status(aiNote.getDraftStatus())
                    .appliedAt(aiNote.getUpdatedAt())
                    .structuredData(resolved.draft())
                    .resolutions(resolved.resolution())
                    .warnings(normalized.warnings())
                    .build();
        } finally {
            AuditContextHolder.clear();
        }
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

    private void validateApplicableStatus(ConsultationAiNoteStatus status) {
        if (status == ConsultationAiNoteStatus.APPLIED) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_ALREADY_APPLIED);
        }
        if (status != ConsultationAiNoteStatus.GPT_COMPLETED) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_NOT_APPLICABLE);
        }
    }

    private String writeStructuredData(ConsultationAiStructuredDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ConsultationErrorCode.CONSULTATION_AI_NOTE_INVALID_DATA);
        }
    }
}
