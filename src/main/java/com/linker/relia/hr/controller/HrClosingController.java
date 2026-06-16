package com.linker.relia.hr.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.hr.dto.HrClosingProcessRequest;
import com.linker.relia.hr.dto.HrClosingProcessResponse;
import com.linker.relia.hr.dto.HrClosingSummaryResponse;
import com.linker.relia.hr.dto.HrClosingUserListResponse;
import com.linker.relia.hr.dto.OrganizationClosingListResponse;
import com.linker.relia.hr.service.HrClosingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr-closings")
public class HrClosingController {
    private final HrClosingService hrClosingService;

    @GetMapping("/{closingMonth}")
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HrClosingSummaryResponse>> getSummary(@PathVariable String closingMonth) {
        HrClosingSummaryResponse response = hrClosingService.getSummary(closingMonth);
        return ApiResponse.success(HttpStatus.OK, "인사 및 조직 마감 요약 조회 성공", response);
    }

    @GetMapping("/{closingMonth}/organizations")
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<OrganizationClosingListResponse>> getOrganizations(@PathVariable String closingMonth) {
        OrganizationClosingListResponse response = hrClosingService.getOrganizations(closingMonth);
        return ApiResponse.success(HttpStatus.OK, "조직 마감 스냅샷 조회 성공", response);
    }

    @GetMapping("/{closingMonth}/users")
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HrClosingUserListResponse>> getUsers(@PathVariable String closingMonth) {
        HrClosingUserListResponse response = hrClosingService.getUsers(closingMonth);
        return ApiResponse.success(HttpStatus.OK, "인사 마감 스냅샷 조회 성공", response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HrClosingProcessResponse>> close(@Valid @RequestBody HrClosingProcessRequest request) {
        HrClosingProcessResponse response = hrClosingService.close(request);
        return ApiResponse.success(HttpStatus.CREATED, "인사 및 조직 마감 처리 성공", response);
    }
}
