package com.linker.relia.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleCalendarResponse {

    private int year;

    private int month;

    private List<ScheduleCalendarDayResponse> days;
}