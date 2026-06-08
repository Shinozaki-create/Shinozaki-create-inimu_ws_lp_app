package com.example.inimuws.dto;

import java.time.LocalDate;

public record ScheduleResponse(
        LocalDate date,
        boolean open,
        int totalCapacity,
        int reservedCount,
        int remainingCount,
        boolean fullyBooked
) {
}
