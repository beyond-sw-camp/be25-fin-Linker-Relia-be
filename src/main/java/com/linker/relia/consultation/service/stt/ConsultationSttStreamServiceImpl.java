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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationSttStreamServiceImpl implements ConsultationSttStreamService {
    private final ConsultationSttSessionService consultationSttSessionService;
    private final ConsultationSttStreamRegistry consultationSttStreamRegistry;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ClovaSpeechGrpcClient clovaSpeechGrpcClient;

    // WebSocket 연결 시 토큰과 세션 소유권을 검증하고 CLOVA gRPC 스트림을 연다.
    @Override
    public void openStream(UUID sessionId, String accessToken, WebSocketSession webSocketSession) throws IOException {
        User user = authenticate(accessToken);
        ConsultationSttSession sttSession = consultationSttSessionService.getOwnedSession(sessionId, user.getId());

        // 세션별 CLOVA gRPC 스트림을 열고 partial/final/error 콜백을 연결한다.
        ClovaSpeechGrpcClient.ClovaSpeechStream clovaSpeechStream = clovaSpeechGrpcClient.openStream(
                sessionId,
                partialText -> {
                    updatePartialText(sessionId, partialText);
                    sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.partialText(sessionId, partialText));
                },
                finalText -> sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.finalText(sessionId, finalText)),
                throwable -> sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.error(sessionId, throwable.getMessage()))
        );

        // 현재 WebSocket 연결과 gRPC 스트림을 세션 기준으로 레지스트리에 등록한다.
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

        // 스트림 연결이 완료되었음을 클라이언트에 알린다.
        sendEventSilently(webSocketSession, ConsultationSttWebSocketEventResponse.connected(sessionId));
    }

    // 수신한 오디오 청크를 현재 세션의 CLOVA gRPC 스트림으로 전달한다.
    @Override
    public void handleAudioChunk(UUID sessionId, byte[] audioBytes) {
        ConsultationSttStreamRegistry.StreamContext context = consultationSttStreamRegistry.get(sessionId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));

        context.getClovaSpeechStream().sendAudio(audioBytes);
    }

    // 세션 종료 시 등록된 gRPC 스트림을 정상 종료하고 레지스트리에서 제거한다.
    @Override
    public void closeStream(UUID sessionId) {
        consultationSttStreamRegistry.remove(sessionId)
                .ifPresent(context -> context.getClovaSpeechStream().complete());
    }

    // 스트림 처리 중 오류가 나면 gRPC 스트림을 취소하고 레지스트리에서 제거한다.
    @Override
    public void failStream(UUID sessionId, Throwable throwable) {
        consultationSttStreamRegistry.remove(sessionId)
                .ifPresent(context -> context.getClovaSpeechStream().cancel(throwable));
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

    // partialText를 세션 엔티티에 반영해 중간 전사 결과를 누적한다.
    private void updatePartialText(UUID sessionId, String partialText) {
        ConsultationSttSession session = consultationSttSessionService.getOwnedSession(sessionId, getFpIdFromRegistry(sessionId));
        session.updatePartialText(partialText);
    }

    // 현재 스트림 레지스트리에서 세션 소유자의 FP ID를 조회한다.
    private UUID getFpIdFromRegistry(UUID sessionId) {
        return consultationSttStreamRegistry.get(sessionId)
                .map(ConsultationSttStreamRegistry.StreamContext::getFpId)
                .orElseThrow(() -> new BusinessException(ConsultationErrorCode.CONSULTATION_STT_SESSION_NOT_FOUND));
    }

    // WebSocket이 열려 있으면 클라이언트로 이벤트 메시지를 전송한다.
    private void sendEventSilently(WebSocketSession webSocketSession, ConsultationSttWebSocketEventResponse event) {
        try {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            }
        } catch (IOException ignored) {
        }
    }
}
