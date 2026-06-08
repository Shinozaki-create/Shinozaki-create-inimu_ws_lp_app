package com.example.inimuws.dto;

import java.time.LocalDate;

public record DailySalesSummary(
        LocalDate date,
        int totalAmount,
        long reservationCount,
        int participantCount
) {
}
