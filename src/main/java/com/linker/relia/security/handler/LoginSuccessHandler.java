package com.linker.relia.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.auth.dto.LoginResponse;
import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.util.CookieUtil;
import com.linker.relia.infra.redis.AuthTokenRepository;
import com.linker.relia.organization.repository.OrganizationRepository;
import com.linker.relia.security.jwt.JwtUtil;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final AuthTokenRepository authTokenRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        String organizationName = organizationRepository.findById(principalDetails.getUser().getOrganizationId())
                .map(organization -> organization.getOrganizationName())
                .orElse(null);

        String accessToken = jwtUtil.createAccessToken(
                principalDetails.getUsername(),
                principalDetails.getUser().getUserRole().name()
        );

        String refreshToken = jwtUtil.createRefreshToken(
                principalDetails.getUsername(),
                principalDetails.getUser().getUserRole().name()
        );

        String refreshTokenId = jwtUtil.getTokenId(refreshToken);

        authTokenRepository.saveRefreshToken(
                refreshTokenId,
                refreshToken,
                jwtUtil.getRefreshExpiredMs()
        );

        Cookie refreshCookie = cookieUtil.createCookie("RefreshToken", refreshToken, 1);
        response.addCookie(refreshCookie);

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .role(principalDetails.getUser().getUserRole().name())
                .userName(principalDetails.getUser().getUserName())
                .organizationName(organizationName)
                .build();

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.successBody(HttpStatus.OK, "로그인에 성공하였습니다.", loginResponse)
        );
    }
}
