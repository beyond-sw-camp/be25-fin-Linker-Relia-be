package com.linker.relia.esgimpact.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.esgimpact.dto.EsgImpactResponse;
import com.linker.relia.esgimpact.service.EsgImpactService;
import com.linker.relia.security.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EsgImpactController {
    private final EsgImpactService esgImpactService;

    @GetMapping("/api/esg-impact/me")
    public ResponseEntity<ApiResponse<EsgImpactResponse>> getMyEsgImpact(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) String targetMonth
    ) {
        EsgImpactResponse response = esgImpactService.getMyImpact(principalDetails, targetMonth);
        return ApiResponse.success(HttpStatus.OK, "ESG Impact 조회 성공", response);
    }
}
