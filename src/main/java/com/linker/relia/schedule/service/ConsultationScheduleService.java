package com.linker.relia.schedule.service;

import com.linker.relia.schedule.dto.request.ConsultationScheduleCreateRequest;
import com.linker.relia.schedule.dto.request.ConsultationScheduleUpdateRequest;
import com.linker.relia.schedule.dto.response.ConsultationScheduleCreateResponse;
import com.linker.relia.schedule.dto.response.ConsultationScheduleListResponse;
import com.linker.relia.schedule.dto.response.ScheduleCalendarResponse;
import com.linker.relia.user.domain.User;

import java.time.LocalDate;
import java.util.UUID;

public interface ConsultationScheduleService {

    ConsultationScheduleCreateResponse createSchedule(
            ConsultationScheduleCreateRequest request,
            User fp
    );

    ConsultationScheduleListResponse getSchedulesByDate(
            LocalDate date,
            User fp
    );

    ConsultationScheduleCreateResponse updateSchedule(
            UUID fpId,
            UUID scheduleId,
            ConsultationScheduleUpdateRequest request
    );

    void deleteSchedule(
            UUID fpId,
            UUID scheduleId
    );

    ScheduleCalendarResponse getMonthlyCalendar(
            int year,
            int month,
            User fp
    );
}