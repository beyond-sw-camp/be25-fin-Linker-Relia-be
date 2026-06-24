package com.linker.relia.notification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long TIMEOUT = 30 * 60 * 1000L;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30L;

    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitter.onCompletion(() -> {
            remove(userId, emitter);
            log.info("SSE connection completed userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
            log.info("SSE connection timed out userId={}", userId);
        });
        emitter.onError(error -> {
            remove(userId, emitter);
            // 브라우저 새로고침/탭 종료는 정상적인 SSE 연결 종료이므로 log.WARN 남기지 않음
            if (isDisconnectedClientException(error)) {
                log.debug("SSE client disconnected userId={}, reason={}", userId, error.getClass().getSimpleName());
                return;
            }
            log.warn("SSE connection error userId={}", userId, error);
        });

        emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        startHeartbeat(userId, emitter);

        log.info("SSE connection registered userId={}, activeConnections={}", userId, activeConnectionCount());

        if (!sendEvent(emitter, "connect", "connected")) {
            remove(userId, emitter);
        }

        return emitter;
    }

    public void send(UUID userId, NotificationEvent event) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.info(
                    "SSE send skipped userId={}, type={}, referenceId={}, reason=offline",
                    userId,
                    event.getType(),
                    event.getReferenceId()
            );
            return;
        }

        userEmitters.forEach(emitter -> {
            if (!sendEvent(emitter, event.getType().name(), event)) {
                remove(userId, emitter);
            }
        });
    }

    public void sendToAll(NotificationEvent event) {
        emitters.keySet().forEach(userId -> send(userId, event));
    }

    @PreDestroy
    public void shutdown() {
        heartbeatTasks.values().forEach(task -> task.cancel(true));
        heartbeatExecutor.shutdownNow();
        emitters.values().forEach(userEmitters -> userEmitters.forEach(SseEmitter::complete));
        emitters.clear();
        heartbeatTasks.clear();
    }

    private void startHeartbeat(UUID userId, SseEmitter emitter) {
        ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    if (!sendEvent(emitter, "ping", Instant.now().toString())) {
                        remove(userId, emitter);
                    }
                },
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        heartbeatTasks.put(emitter, task);
    }

    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            // 연결 종료 후 heartbeat/data 전송 실패는 예상 가능한 상황이며, 호출부에서 emitter를 제거한다.
            if (isDisconnectedClientException(e)) {
                log.debug("SSE send skipped for disconnected client eventName={}, reason={}", eventName, e.getClass().getSimpleName());
                return false;
            }
            log.warn("SSE send failed eventName={}", eventName, e);
            return false;
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }

        ScheduledFuture<?> task = heartbeatTasks.remove(emitter);
        if (task != null) {
            task.cancel(true);
        }
    }

    private int activeConnectionCount() {
        return emitters.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    private boolean isDisconnectedClientException(Throwable error) {
        if (error instanceof IOException
                || error instanceof AsyncRequestNotUsableException
                || error instanceof IllegalStateException) {
            return true;
        }

        Throwable cause = error.getCause();
        return cause != null && cause != error && isDisconnectedClientException(cause);
    }
}
