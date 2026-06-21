package com.linker.relia.consultation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ConsultationSttWebSocketEventResponse {
    private String type;
    private UUID sessionId;
    private String text;
    private String message;

    public static ConsultationSttWebSocketEventResponse connected(UUID sessionId) {
        return ConsultationSttWebSocketEventResponse.builder()
                .type("CONNECTED")
                .sessionId(sessionId)
                .message("audio stream connected")
                .build();
    }

    public static ConsultationSttWebSocketEventResponse partialText(UUID sessionId, String text) {
        return ConsultationSttWebSocketEventResponse.builder()
                .type("PARTIAL_TEXT")
                .sessionId(sessionId)
                .text(text)
                .build();
    }

    public static ConsultationSttWebSocketEventResponse finalText(UUID sessionId, String text) {
        return ConsultationSttWebSocketEventResponse.builder()
                .type("FINAL_TEXT")
                .sessionId(sessionId)
                .text(text)
                .build();
    }

    public static ConsultationSttWebSocketEventResponse error(UUID sessionId, String message) {
        return ConsultationSttWebSocketEventResponse.builder()
                .type("ERROR")
                .sessionId(sessionId)
                .message(message)
                .build();
    }
}
