package com.linker.relia.schedule.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.schedule.dto.request.ConsultationScheduleCreateRequest;
import com.linker.relia.schedule.dto.request.ConsultationScheduleUpdateRequest;
import com.linker.relia.schedule.dto.response.ConsultationScheduleCreateResponse;
import com.linker.relia.schedule.dto.response.ConsultationScheduleListResponse;
import com.linker.relia.schedule.service.ConsultationScheduleService;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsultationScheduleController {

    private final ConsultationScheduleService consultationScheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationScheduleCreateResponse>> createSchedule(
            @Valid @RequestBody ConsultationScheduleCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationScheduleCreateResponse response =
                consultationScheduleService.createSchedule(request, fp);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "상담 일정이 등록되었습니다.",
                response
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ConsultationScheduleListResponse>> getSchedulesByDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationScheduleListResponse response =
                consultationScheduleService.getSchedulesByDate(date, fp);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담 일정 조회에 성공했습니다.",
                response
        );
    }

    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ConsultationScheduleCreateResponse>> updateSchedule(
            @PathVariable UUID scheduleId,
            @RequestBody ConsultationScheduleUpdateRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationScheduleCreateResponse response =
                consultationScheduleService.updateSchedule(fp.getId(), scheduleId, request);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담 일정이 수정되었습니다.",
                response
        );
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @PathVariable UUID scheduleId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        consultationScheduleService.deleteSchedule(fp.getId(), scheduleId);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담 일정이 삭제되었습니다.",
                null
        );
    }
}