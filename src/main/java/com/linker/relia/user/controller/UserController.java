package com.linker.relia.user.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.user.dto.FpSignupRequest;
import com.linker.relia.user.dto.FpSignupResponse;
import com.linker.relia.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<FpSignupResponse>> createFpUser(@Valid @RequestBody FpSignupRequest request) {
        FpSignupResponse responseDto = userService.createFpUser(request);
        return ApiResponse.success(HttpStatus.CREATED, "설계사 회원가입이 완료되었습니다.", responseDto);
    }
}
