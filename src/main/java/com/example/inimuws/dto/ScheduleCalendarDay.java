package com.example.inimuws.dto;

import java.time.LocalDate;
import java.util.List;

public record ScheduleCalendarDay(
        LocalDate date,
        boolean currentMonth,
        boolean hasSchedule,
        boolean open,
        boolean fullyBooked,
        List<TimeSlotResponse> timeSlots
) {
}
