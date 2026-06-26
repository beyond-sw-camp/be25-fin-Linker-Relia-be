package com.linker.relia.infra.config;

import com.linker.relia.consultation.websocket.ConsultationSttAudioWebSocketHandler;
import com.linker.relia.consultation.websocket.ConsultationSttHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ConsultationSttAudioWebSocketHandler consultationSttAudioWebSocketHandler;
    private final ConsultationSttHandshakeInterceptor consultationSttHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consultationSttAudioWebSocketHandler, "/ws/consultation-stt/audio/*")
                .addInterceptors(consultationSttHandshakeInterceptor)
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://d1mht2pok8se28.cloudfront.net",
                        "https://relireli.org",
                        "https://www.relireli.org"
                );
    }
}
