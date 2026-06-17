package com.linker.relia.common.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmEventPublisher {

    private final SseEmitterManager sseEmitterManager;

    public void publish(AlarmEvent event) {
        sseEmitterManager.send(event.getReceiverUserId(), event);
    }
}
