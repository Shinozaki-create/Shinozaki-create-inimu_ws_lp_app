package com.example.inimuws.dto;

import java.time.LocalDate;

public record DashboardCalendarDay(
        LocalDate date,
        boolean currentMonth,
        boolean hasSchedule,
        boolean open,
        int reservedCount,
        String label
) {
}
