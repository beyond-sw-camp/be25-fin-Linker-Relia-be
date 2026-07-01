package com.linker.relia.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.exception.ErrorCode;
import com.linker.relia.user.exception.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String message;
        ErrorCode errorCode;
        if (exception instanceof AuthenticationServiceException
                && exception.getCause() instanceof IOException) {
            message = AuthErrorCode.INVALID_LOGIN_REQUEST.getMessage();
            errorCode = AuthErrorCode.INVALID_LOGIN_REQUEST;
        } else if (exception instanceof LockedException || exception instanceof DisabledException) {
            message = UserErrorCode.USER_RESIGNED.getMessage();
            errorCode = UserErrorCode.USER_RESIGNED;
        } else {
            message = AuthErrorCode.INVALID_LOGIN_CREDENTIALS.getMessage();
            errorCode = AuthErrorCode.INVALID_LOGIN_CREDENTIALS;
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.failureBody(errorCode, message));
    }
}
