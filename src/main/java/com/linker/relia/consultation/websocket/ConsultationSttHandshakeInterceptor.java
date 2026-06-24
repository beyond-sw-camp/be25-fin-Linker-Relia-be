package com.linker.relia.consultation.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

// 연결 전에 sessionId, token을 뽑아 WebSocket attributes에 저장
@Component
public class ConsultationSttHandshakeInterceptor implements HandshakeInterceptor {

    public static final String SESSION_ID_ATTRIBUTE = "consultationSttSessionId";
    public static final String ACCESS_TOKEN_ATTRIBUTE = "consultationSttAccessToken";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        // WebSocket 연결 전에 경로의 sessionId와 쿼리의 token을 추출해 속성으로 저장한다.
        URI uri = request.getURI();
        String path = uri.getPath();
        String sessionIdText = path.substring(path.lastIndexOf('/') + 1);

        try {
            attributes.put(SESSION_ID_ATTRIBUTE, UUID.fromString(sessionIdText));
        } catch (IllegalArgumentException exception) {
            return false;
        }

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            attributes.put(ACCESS_TOKEN_ATTRIBUTE, token);
        }

        return true;
    }

    // handshake 이후에는 별도 후처리가 없어 비워둔다.
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
