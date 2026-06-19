package com.linker.relia.monthlyclosing.scheduler;

import com.linker.relia.monthlyclosing.service.MonthlyClosingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyClosingScheduler {
    private final MonthlyClosingService monthlyClosingService;

    @Value("${app.monthly-closing.zone:Asia/Seoul}")
    private String monthlyClosingZone;

    @Scheduled(
            cron = "${app.monthly-closing.cron:0 0 3 1 * *}",
            zone = "${app.monthly-closing.zone:Asia/Seoul}"
    )
    public void runPreviousMonthClosing() {
        YearMonth closingMonth = YearMonth.now(ZoneId.of(monthlyClosingZone)).minusMonths(1);
        log.info("월 마감 스케줄러가 실행되었습니다. closingMonth={}", closingMonth);
        monthlyClosingService.runPreviousMonthClosing(closingMonth);
    }
}
