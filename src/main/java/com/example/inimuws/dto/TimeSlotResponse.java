package com.example.inimuws.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record TimeSlotResponse(
        Long slotId,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        int capacity,
        int reservedCount,
        int remainingCount,
        boolean active,
        boolean fullyBooked
) {
}
