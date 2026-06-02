package com.linker.relia.auth.service;

import com.linker.relia.auth.dto.ReissueResponse;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.ErrorCode;
import com.linker.relia.common.util.CookieUtil;
import com.linker.relia.infra.redis.AuthTokenRepository;
import com.linker.relia.security.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtUtil jwtUtil;
    private final AuthTokenRepository authTokenRepository;
    private final CookieUtil cookieUtil;

    @Override
    public ReissueResponse reissueToken(String refreshToken, HttpServletResponse response) {
        // 리프레시 토큰이 없는 경우
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 토큰 만료 확인
        if (jwtUtil.isExpired(refreshToken)) {
            response.addCookie(cookieUtil.deleteCookie("RefreshToken"));
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 리프레시 토큰인지 확인
        if (!"RefreshToken".equals(jwtUtil.getCategory(refreshToken))) {
            response.addCookie(cookieUtil.deleteCookie("RefreshToken"));
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String loginId = jwtUtil.getLoginId(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String redisKey = jwtUtil.getTokenId(refreshToken);

        if (!authTokenRepository.hasRefreshToken(redisKey)) {
            response.addCookie(cookieUtil.deleteCookie("RefreshToken"));
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_IN_REDIS);
        }

        // 새로운 액세스 토큰과 리프레시 토큰 생성
        String newAccessToken = jwtUtil.createAccessToken(loginId, role);
        String newRefreshToken = jwtUtil.createRefreshToken(loginId, role);

        // 새 리프레시 토큰의 jti 추출
        String newRefreshJti = jwtUtil.getTokenId(newRefreshToken);

        authTokenRepository.saveRefreshToken(newRefreshJti, newRefreshToken, jwtUtil.getRefreshExpiredMs());

        // 기존 리프레시 토큰 삭제
        authTokenRepository.deleteRefreshToken(redisKey);

        // 쿠키에 새 리프레시 토큰 저장
        Cookie refreshCookie = cookieUtil.createCookie("RefreshToken", newRefreshToken, 1);
        response.addCookie(refreshCookie);

        return ReissueResponse.builder().newAccessToken(newAccessToken).build();
    }

    @Override
    @Transactional
    public void logout(String bearerToken, String refreshToken, HttpServletResponse response) {
        String accessToken = null;

        if (bearerToken != null && !bearerToken.isBlank() && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7); // "Bearer " 제거
        }

        String accessTokenId = jwtUtil.getTokenId(accessToken);
        String refreshTokenId = jwtUtil.getTokenId(refreshToken);
        long remainingMillis = jwtUtil.getRemainingExpirationMillis(accessToken);

        if (remainingMillis > 0) {
            authTokenRepository.saveBlacklist(accessTokenId, remainingMillis);
            // 동일한 tokenId로 저장된 리프레시 토큰은 로그아웃 시 함께 제거한다.
            authTokenRepository.deleteRefreshToken(refreshTokenId);
        }

        // 쿠키에서 refresh token 제거
        Cookie refreshCookie = cookieUtil.createCookie("RefreshToken", null, 0);

        response.addCookie(refreshCookie);
    }
}
