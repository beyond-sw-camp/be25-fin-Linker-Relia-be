package com.linker.relia.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ConsultationScheduleListResponse {

    private LocalDate date;

    private List<ConsultationScheduleItemResponse> schedules;
}