package com.linker.relia.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        writeUnauthorizedResponse(response, AuthErrorCode.USER_UNAUTHORIZED, null);
    }

    public void commence(HttpServletRequest request, HttpServletResponse response, BusinessException exception) throws IOException {
        ErrorCode errorCode = exception.getErrorCode() == null ? AuthErrorCode.USER_UNAUTHORIZED : exception.getErrorCode();
        writeUnauthorizedResponse(response, errorCode, exception.getMessage());
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failureBody(errorCode, message));
    }
}
