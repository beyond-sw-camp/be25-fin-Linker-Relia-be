package com.linker.relia.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ScheduleCalendarDayResponse {

    private LocalDate date;

    private long consultationCount;

    private long contractExpiryCount;
}