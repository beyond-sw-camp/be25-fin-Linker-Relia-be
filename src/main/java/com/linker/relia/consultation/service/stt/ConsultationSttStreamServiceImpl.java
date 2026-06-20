package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.consultation.domain.stt.ConsultationSttSession;
import com.linker.relia.consultation.dto.response.ConsultationSttWebSocketEventResponse;
import com.linker.relia.consultation.exception.ConsultationErrorCode;
import com.linker.relia.infra.clova.ClovaSpeechGrpcClient;
import com.linker.relia.security.jwt.JwtUtil;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultationSttStreamServiceImpl implements ConsultationSttStreamService {
    private final ConsultationAiNoteService consultationAiNoteService;
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationSttStreamRegistry consultationSttStreamRegistry;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ClovaSpeechGrpcClient clovaSpeechGrpcClient;

    @Override
    public void openStream(UUID sessionId, String accessToken, WebSocketSession webSocketSession) throws IOException {
        User user = authenticate(accessToken);
        ConsultationSttSession sttSession = consultationSttSessionService.getOwnedSession(sessionId, user.getId());

        ClovaSpeechGrpcClient.ClovaSpeechStream clovaSpeechStream = clovaSpeechGrpcClient.openStream(
                sessionId,
                partialText -> {
                    updatePartialText(sessionId, partialText);
                    sendEventSilently(
                            webSocketSession,
                            ConsultationSttWebSocketEventResponse.partialText(sessionId, partialText)
                    );
                },
                finalText -> {
                    completeSession(sessionId, finalText);
                    sendEventSilently(
                            webSocketSession,
                            ConsultationSttWebSocketEventResponse.finalText(sessionId, finalText)
                    );
                },
                throwable -> {
                    failSession(sessionId, throwable);
                    sendEventSilently(
                            webSocketSession,
                            ConsultationSttWebSocketEventResponse.error(sessionId, throwable.getMessage())
                    );
                }
        );

        consultationSttStreamRegistry.register(
                sessionId,
                ConsultationSttStreamRegistry.StreamContext.builder()
                        .sessionId(sessionId)
                        .fpId(sttSession.getFp().getId())
                        .webSocketSession(webSocketSession)
                        .clovaSpeechStream(clovaSpeechStream)
                        .connectedAt(LocalDateTime.now())
                        .build()
        );

        sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.connected(sessionId));
    }

    @Override
    public void handleAudioChunk(UUID sessionId, byte[] audioBytes) {
        ConsultationSttStreamRegistry.StreamContext context = getRequiredContext(sessionId);
        context.getClovaSpeechStream().sendAudio(audioBytes);
    }

    @Override
    public void closeStream(UUID sessionId) {
        ConsultationSttStreamRegistry.StreamContext context = consultationSttStreamRegistry.get(sessionId)
                .orElse(null);
        if (context == null) {
            log.warn("활성화된 STT 스트림 컨텍스트가 없어 closeStream을 건너뜁니다. sessionId={}", sessionId);
            return;
        }

        ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, context.getFpId());
        session.markProcessing();
        context.getClovaSpeechStream().complete();
    }

    @Override
    public void failStream(UUID sessionId, Throwable throwable) {
        failSession(sessionId, throwable);
    }

    private User authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND);
        }

        String loginId = jwtUtil.getLoginId(accessToken);
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_UNAUTHORIZED));
    }

    private void updatePartialText(UUID sessionId, String partialText) {
        ConsultationSttStreamRegistry.StreamContext context = getRequiredContext(sessionId);
        consultationSttSessionService.updatePartialText(sessionId, context.getFpId(), partialText);
    }

    private void completeSession(UUID sessionId, String finalText) {
        ConsultationSttStreamRegistry.StreamContext context = consultationSttStreamRegistry.remove(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));

        consultationSttSessionService.completeSession(
                sessionId,
                context.getFpId(),
                finalText,
                LocalDateTime.now()
        );
        consultationAiNoteService.processSttCompleted(sessionId, context.getFpId(), finalText);
    }

    private void failSession(UUID sessionId, Throwable throwable) {
        consultationSttStreamRegistry.remove(sessionId)
                .ifPresent(context -> {
                    String errorMessage = throwable.getMessage() != null
                            ? throwable.getMessage()
                            : "실시간 STT 처리 중 오류가 발생했습니다.";
                    consultationSttSessionService.failSession(
                            sessionId,
                            context.getFpId(),
                            errorMessage,
                            LocalDateTime.now()
                    );
                });
    }

    private ConsultationSttStreamRegistry.StreamContext getRequiredContext(UUID sessionId) {
        return consultationSttStreamRegistry.get(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));
    }

    private void sendEventSilently(WebSocketSession webSocketSession, ConsultationSttWebSocketEventResponse event) {
        try {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            }
        } catch (IOException ignored) {
        }
    }
}
