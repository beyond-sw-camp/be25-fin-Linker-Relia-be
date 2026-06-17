package com.linker.relia.handover.controller;

import com.linker.relia.common.dto.request.PageQueryRequest;
import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.common.dto.response.PageResponse;
import com.linker.relia.handover.domain.RequestStatus;
import com.linker.relia.handover.domain.RequestType;
import com.linker.relia.handover.dto.request.HandoverApprovalRequest;
import com.linker.relia.handover.dto.request.HandoverAssignRequest;
import com.linker.relia.handover.dto.request.HandoverCreateRequest;
import com.linker.relia.handover.dto.response.HandoverAssignableFpResponse;
import com.linker.relia.handover.dto.response.HandoverCreateResponse;
import com.linker.relia.handover.dto.response.HandoverDetailResponse;
import com.linker.relia.handover.dto.response.HandoverListItemResponse;
import com.linker.relia.handover.dto.response.HandoverReceivedItemResponse;
import com.linker.relia.handover.dto.response.HandoverReceivedSummaryResponse;
import com.linker.relia.handover.dto.response.HandoverSummaryResponse;
import com.linker.relia.handover.service.HandoverService;
import com.linker.relia.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/handovers")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    // 인수인계 요청 생성
    @PostMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HandoverCreateResponse>> createHandover(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody HandoverCreateRequest request) {

        HandoverCreateResponse response = handoverService.createHandover(principal, request);
        return ApiResponse.success(HttpStatus.CREATED, "인수인계 요청 생성 성공", response);
    }

    // 인수인계 요청 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<HandoverListItemResponse>>> getHandoverList(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType requestType,
            @RequestParam(required = false) String customerName,
            @Valid @ModelAttribute PageQueryRequest pageRequest) {

        PageResponse<HandoverListItemResponse> response = handoverService
                .getHandoverList(principal, status, requestType, customerName, pageRequest.toPageable());

        return ApiResponse.success(HttpStatus.OK, "인수인계 요청 목록 조회 성공", response);
    }

    //인수인계 상세 조회
    @GetMapping("/{handoverRequestId}")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN', 'FP')")
    public ResponseEntity<ApiResponse<HandoverDetailResponse>> getHandoverDetail(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable UUID handoverRequestId) {

        HandoverDetailResponse response = handoverService
                .getHandoverDetail(principal, handoverRequestId);

        return ApiResponse.success(HttpStatus.OK, "인수인계 요청 상세 조회 성공", response);
    }

    // 인수인계 지점장 결재
    @PatchMapping("/{handoverRequestId}/approval")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> processApproval(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable UUID handoverRequestId,
            @RequestBody HandoverApprovalRequest request) {

        handoverService.processApproval(principal, handoverRequestId, request);
        return ApiResponse.success(HttpStatus.OK, "결재 처리 완료", null);
    }

    // 받은 인수인계 목록
    @GetMapping("/received")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<PageResponse<HandoverReceivedItemResponse>>> getReceivedList(
            @AuthenticationPrincipal PrincipalDetails principal,
            @Valid @ModelAttribute PageQueryRequest pageRequest) {

        PageResponse<HandoverReceivedItemResponse> response = handoverService
                .getReceivedList(principal, pageRequest.toPageable());

        return ApiResponse.success(HttpStatus.OK, "인수받은 고객 목록 조회 성공", response);
    }

    // 인수인계 요청 목록 요약
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'HQ_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<HandoverSummaryResponse>> getSummary(
            @AuthenticationPrincipal PrincipalDetails principal) {

        HandoverSummaryResponse response = handoverService.getSummary(principal);
        return ApiResponse.success(HttpStatus.OK, "인수인계 요약 조회 성공", response);
    }

    // 받은 인수인계 목록 요약
    @GetMapping("/received/summary")
    @PreAuthorize("hasRole('FP')")
    public ResponseEntity<ApiResponse<HandoverReceivedSummaryResponse>> getReceivedSummary(
            @AuthenticationPrincipal PrincipalDetails principal) {

        HandoverReceivedSummaryResponse response = handoverService.getReceivedSummary(principal);
        return ApiResponse.success(HttpStatus.OK, "인수받은 목록 요약 조회 성공", response);
    }

    // 지정 가능한 설계사 목록 API
    @GetMapping("/{handoverRequestId}/assignable-fps")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<HandoverAssignableFpResponse>>> getAssignableFps(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable UUID handoverRequestId,
            @Valid @ModelAttribute PageQueryRequest pageRequest) {

        PageResponse<HandoverAssignableFpResponse> response = handoverService
                .getAssignableFps(principal, handoverRequestId, pageRequest.toPageable());

        return ApiResponse.success(HttpStatus.OK, "직접 지정 가능 설계사 목록 조회 성공", response);
    }

    // 설계사 직접 지정
    @PostMapping("/{handoverRequestId}/assign")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> processAssign(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable UUID handoverRequestId,
            @Valid @RequestBody HandoverAssignRequest request) {

        handoverService.processAssign(principal, handoverRequestId, request);
        return ApiResponse.success(HttpStatus.OK, "설계사 직접 지정 완료", null);
    }



}
