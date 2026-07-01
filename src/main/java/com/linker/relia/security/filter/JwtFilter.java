package com.linker.relia.security.filter;

import com.linker.relia.auth.exception.AuthErrorCode;
import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.infra.redis.AuthTokenRepository;
import com.linker.relia.security.handler.CustomAuthenticationEntryPoint;
import com.linker.relia.security.jwt.JwtUtil;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.security.principal.PrincipalDetailsService;
import com.linker.relia.user.exception.UserErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final PrincipalDetailsService principalDetailsService;
    private final AuthTokenRepository authTokenRepository;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            if (requestUri.startsWith("/api/actuator/health")) {
                filterChain.doFilter(request, response);
                return;
            }

            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                // JWT가 없는 요청 → 인증 대상이 아니므로 그대로 다음 필터로 전달
                filterChain.doFilter(request, response);

                // 이후 JWT 검증 로직을 수행하지 않고 현재 필터 종료
                return;
            }

            String accessToken = authorizationHeader.substring(7); // "Bearer " 이후의 토큰만 추출

            if (jwtUtil.isExpired(accessToken)) {
                throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_EXPIRED);
            }

            String category = jwtUtil.getCategory(accessToken);
            if (!"AccessToken".equals(category)) {
                throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_INVALID);
            }

            String tokenId = jwtUtil.getTokenId(accessToken);
            if (authTokenRepository.hasBlacklistedToken(tokenId)) {
                throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_OF_BLACK_LIST);
            }

            String loginId = jwtUtil.getLoginId(accessToken);
            PrincipalDetails principal = (PrincipalDetails) principalDetailsService.loadUserByUsername(loginId);
            if (!principal.isEnabled()) {
                throw new BusinessException(UserErrorCode.USER_RESIGNED);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            customAuthenticationEntryPoint.commence(request, response, exception);
        }
    }
}
