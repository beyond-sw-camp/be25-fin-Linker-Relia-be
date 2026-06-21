package com.linker.relia.consultation.service.stt;

import com.linker.relia.infra.clova.ClovaSpeechGrpcClient.ClovaSpeechStream;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ConsultationSttStreamRegistry {

    private final ConcurrentMap<UUID, StreamContext> contexts = new ConcurrentHashMap<>();

    public void register(UUID sessionId, StreamContext context) {
        contexts.put(sessionId, context);
    }

    public Optional<StreamContext> get(UUID sessionId) {
        return Optional.ofNullable(contexts.get(sessionId));
    }

    public Optional<StreamContext> remove(UUID sessionId) {
        return Optional.ofNullable(contexts.remove(sessionId));
    }

    @Getter
    @Builder
    public static class StreamContext {
        private final UUID sessionId;
        private final UUID fpId;
        private final WebSocketSession webSocketSession;
        private final ClovaSpeechStream clovaSpeechStream;
        private final LocalDateTime connectedAt;
    }
}
