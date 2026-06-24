package com.linker.relia.auth.controller;

import com.linker.relia.auth.dto.LoginRequest;
import com.linker.relia.auth.dto.ReissueResponse;
import com.linker.relia.auth.service.AuthService;
import com.linker.relia.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "로그인", description = "Spring Security 필터에서 처리되는 로그인 API입니다.")
    @PostMapping("/login")
    public void login(@RequestBody LoginRequest request) {
        throw new UnsupportedOperationException("Swagger 문서화 전용 API입니다.");
    }


    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissueToken(@CookieValue(name = "RefreshToken", defaultValue = "") String refreshToken,
                                                                     HttpServletResponse response) {
        ReissueResponse responseDto = authService.reissueToken(refreshToken, response);
        return ApiResponse.success(HttpStatus.OK, "토큰 재발급을 성공했습니다.", responseDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(name = "Authorization", required = false) String bearerToken,
                                                    @CookieValue(name = "RefreshToken", defaultValue = "") String refreshToken,
                                                    HttpServletResponse httpServletResponse) {
        authService.logout(bearerToken, refreshToken, httpServletResponse);
        return ApiResponse.success(HttpStatus.OK, "로그아웃에 성공했습니다.", null);
    }
}
