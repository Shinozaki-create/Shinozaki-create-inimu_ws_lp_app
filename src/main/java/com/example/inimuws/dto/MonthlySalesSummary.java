package com.example.inimuws.dto;

import java.time.YearMonth;

public record MonthlySalesSummary(
        YearMonth month,
        int totalAmount,
        long reservationCount,
        int participantCount
) {
}
