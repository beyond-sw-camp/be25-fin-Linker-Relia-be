package com.linker.relia.common.sse;

import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal PrincipalDetails princial){
        UUID userId = princial.getUser().getId();
        return sseEmitterManager.subscribe(userId);
    }
}
