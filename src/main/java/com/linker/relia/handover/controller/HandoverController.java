package com.linker.relia.handover.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.handover.dto.request.HandoverCreateRequest;
import com.linker.relia.handover.dto.response.HandoverCreateResponse;
import com.linker.relia.handover.service.HandoverService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    @PostMapping
    public ResponseEntity<ApiResponse<HandoverCreateResponse>> createHandover(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody HandoverCreateRequest request) {

        HandoverCreateResponse response = handoverService.createHandover(request);
        return ApiResponse.success(HttpStatus.CREATED, "인수인계 요청 생성 성공", response);
    }
}
