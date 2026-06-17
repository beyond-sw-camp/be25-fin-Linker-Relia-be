package com.linker.relia.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final long TIMEOUT = 5 * 60 * 1000L;  // 5분

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 연결 종료/에러 시 자동 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);

        // 연결 직후 더미 이벤트 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    public void send(UUID userId, AlarmEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return; // 오프라인이면 무시

        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType().name())
                    .data(event));
        } catch (IOException e) {
            log.warn("SSE 전송 실패 userId={}", userId, e);
            emitters.remove(userId);
        }
    }
    public void sendToAll(AlarmEvent event){
        emitters.forEach((userId, emitter) -> send(userId, event));
    }
}


