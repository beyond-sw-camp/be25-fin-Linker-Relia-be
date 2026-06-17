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
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE 연결 종료 userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
            log.info("SSE 연결 타임아웃 userId={}", userId);
        });

        emitters.put(userId, emitter);
        log.info("SSE 연결 등록 userId={}, activeConnections={}", userId, emitters.size());

        // 연결 직후 더미 이벤트 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            log.info("SSE connect 이벤트 전송 성공 userId={}", userId);
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE connect 이벤트 전송 실패 userId={}", userId, e);
        }

        return emitter;
    }

    public void send(UUID userId, AlarmEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.info(
                    "SSE 전송 생략 userId={}, type={}, referenceId={}, reason=offline",
                    userId,
                    event.getType(),
                    event.getReferenceId()
            );
            return; // 오프라인이면 무시
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(event.getType().name())
                    .data(event));
            log.info(
                    "SSE 전송 성공 userId={}, type={}, referenceId={}, message={}",
                    userId,
                    event.getType(),
                    event.getReferenceId(),
                    event.getMessage()
            );
        } catch (IOException e) {
            log.warn(
                    "SSE 전송 실패 userId={}, type={}, referenceId={}",
                    userId,
                    event.getType(),
                    event.getReferenceId(),
                    e
            );
            emitters.remove(userId);
        }
    }
    public void sendToAll(AlarmEvent event){
        emitters.forEach((userId, emitter) -> send(userId, event));
    }
}


