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
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
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
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationSttStreamRegistry consultationSttStreamRegistry;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ClovaSpeechGrpcClient clovaSpeechGrpcClient;

    // WebSocket 연결 시 JWT와 세션 소유권을 검증하고 CLOVA gRPC 스트림을 연다.
    @Override
    public void openStream(UUID sessionId, String accessToken, WebSocketSession webSocketSession) throws IOException {
        User user = authenticate(accessToken);
        ConsultationSttSession sttSession = consultationSttSessionService.getOwnedSession(sessionId, user.getId());

        // partial/final/error 콜백에서 세션 상태를 반영하고 브라우저로 이벤트를 전달한다.
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

        // WebSocket 세션과 gRPC 스트림을 세션 ID 기준으로 보관한다.
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

        // 브라우저 쪽에 실시간 STT 연결 준비 완료 이벤트를 보낸다.
        sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.connected(sessionId));
    }

    // 브라우저가 보낸 오디오 청크를 현재 세션의 CLOVA gRPC 스트림으로 전달한다.
    @Override
    public void handleAudioChunk(UUID sessionId, byte[] audioBytes) {
        ConsultationSttStreamRegistry.StreamContext context = getRequiredContext(sessionId);
        context.getClovaSpeechStream().sendAudio(audioBytes);
    }

    // COMPLETE 제어 메시지를 받으면 세션을 PROCESSING으로 전환하고 최종 flush를 요청한다.
    @Override
    public void closeStream(UUID sessionId) {
        ConsultationSttStreamRegistry.StreamContext context = consultationSttStreamRegistry.get(sessionId)
                .orElse(null);
        if (context == null) {
            log.warn("활성화된 STT 스트림 컨텍스트가 없어 closeStream을 건너뜁니다. sessionId={}", sessionId);
            return;
        }

        log.info("STT 스트림 종료 처리를 시작합니다. sessionId={}, fpId={}", sessionId, context.getFpId());
        ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, context.getFpId());
        session.markProcessing();
        log.info("gRPC 종료 전에 STT 세션 상태를 PROCESSING으로 변경했습니다. sessionId={}", sessionId);
        context.getClovaSpeechStream().complete();
        log.info("CLOVA gRPC 종료 요청을 전달했습니다. sessionId={}", sessionId);
    }

    // 스트림 처리 중 오류가 나면 세션과 레지스트리를 함께 실패 상태로 정리한다.
    @Override
    public void failStream(UUID sessionId, Throwable throwable) {
        failSession(sessionId, throwable);
    }

    // WebSocket 연결에 전달된 access token으로 사용자를 인증한다.
    private User authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND);
        }

        String loginId = jwtUtil.getLoginId(accessToken);
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_UNAUTHORIZED));
    }

    // partialText는 별도 트랜잭션 메서드를 통해 저장해 gRPC 콜백 스레드에서도 반영되게 한다.
    private void updatePartialText(UUID sessionId, String partialText) {
        ConsultationSttStreamRegistry.StreamContext context = getRequiredContext(sessionId);
        consultationSttSessionService.updatePartialText(sessionId, context.getFpId(), partialText);
    }

    // finalText가 도착하면 레지스트리에서 스트림을 제거하고 세션을 COMPLETED로 마감한다.
    private void completeSession(UUID sessionId, String finalText) {
        ConsultationSttStreamRegistry.StreamContext context = consultationSttStreamRegistry.remove(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));

        consultationSttSessionService.completeSession(
                sessionId,
                context.getFpId(),
                finalText,
                LocalDateTime.now()
        );
    }

    // 예외 메시지를 세션에 남기고 FAILED로 저장한 뒤 레지스트리에서도 제거한다.
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

    // 레지스트리에 등록된 활성 스트림 컨텍스트를 조회한다.
    private ConsultationSttStreamRegistry.StreamContext getRequiredContext(UUID sessionId) {
        return consultationSttStreamRegistry.get(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));
    }

    // WebSocket이 열려 있으면 브라우저로 STT 이벤트를 직렬화해 전달한다.
    private void sendEventSilently(WebSocketSession webSocketSession, ConsultationSttWebSocketEventResponse event) {
        try {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            }
        } catch (IOException ignored) {
        }
    }
}
