package com.linker.relia.consultation.service.stt;

import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

public interface ConsultationSttStreamService {
    void openStream(UUID sessionId, String accessToken, WebSocketSession webSocketSession) throws IOException;

    void handleAudioChunk(UUID sessionId, byte[] audioBytes);

    void closeStream(UUID sessionId);

    void failStream(UUID sessionId, Throwable throwable);
}
