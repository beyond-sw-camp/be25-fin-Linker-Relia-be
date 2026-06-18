package com.linker.relia.consultation.websocket;

import com.linker.relia.consultation.service.stt.ConsultationSttStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

// 연결 수립, 바이너리 청크 수신, COMPLETE 메시지 처리, 오류/종료 정리 담당
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationSttAudioWebSocketHandler extends BinaryWebSocketHandler {
    private final ConsultationSttStreamService consultationSttStreamService;

    // WebSocket 연결이 수립되면 세션 정보와 토큰으로 STT 스트림을 연다.
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID sessionId = getSessionId(session);
        String accessToken = getAccessToken(session);
        consultationSttStreamService.openStream(sessionId, accessToken, session);
    }

    // 브라우저에서 보낸 오디오 바이너리 청크를 STT 스트림 서비스로 전달한다.
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        UUID sessionId = getSessionId(session);
        ByteBuffer payload = message.getPayload();
        byte[] audioBytes = new byte[payload.remaining()];
        payload.get(audioBytes);
        consultationSttStreamService.handleAudioChunk(sessionId, audioBytes);
    }

    // COMPLETE 제어 메시지를 받으면 현재 세션의 STT 스트림을 종료한다.
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID sessionId = getSessionId(session);
        log.info("STT WebSocket 텍스트 메시지를 수신했습니다. sessionId={}, payload={}", sessionId, message.getPayload());
        if ("COMPLETE".equalsIgnoreCase(message.getPayload())) {
            log.info("STT COMPLETE 종료 신호를 확인했습니다. sessionId={}", sessionId);
            consultationSttStreamService.closeStream(sessionId);
        }
    }

    // 전송 중 오류가 발생하면 로그를 남기고 세션 스트림을 실패 처리한다.
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        UUID sessionId = getSessionId(session);
        log.warn("STT 오디오 WebSocket 전송 오류가 발생했습니다. sessionId={}", sessionId, exception);
        consultationSttStreamService.failStream(sessionId, exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // WebSocket 연결이 닫히면 세션 스트림도 함께 종료한다.
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        consultationSttStreamService.closeStream(getSessionId(session));
    }

    // handshake 단계에서 저장해둔 sessionId 속성을 꺼낸다.
    private UUID getSessionId(WebSocketSession session) {
        Object value = session.getAttributes().get(ConsultationSttHandshakeInterceptor.SESSION_ID_ATTRIBUTE);
        if (value instanceof UUID sessionId) {
            return sessionId;
        }
        throw new IllegalStateException("STT 세션 ID가 존재하지 않습니다.");
    }

    // handshake 단계에서 저장해둔 access token 속성을 꺼낸다.
    private String getAccessToken(WebSocketSession session) throws IOException {
        Object value = session.getAttributes().get(ConsultationSttHandshakeInterceptor.ACCESS_TOKEN_ATTRIBUTE);
        if (value instanceof String accessToken && !accessToken.isBlank()) {
            return accessToken;
        }

        if (session.isOpen()) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
        throw new IllegalStateException("WebSocket access token이 존재하지 않습니다.");
    }
}
