package com.example.inimuws.controller.api;

import com.example.inimuws.dto.ScheduleResponse;
import com.example.inimuws.dto.TimeSlotResponse;
import com.example.inimuws.service.ScheduleService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class PublicScheduleApiController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> schedules() {
        return scheduleService.getPublicSchedules();
    }

    @GetMapping("/{date}/slots")
    public List<TimeSlotResponse> slots(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return scheduleService.getPublicTimeSlots(date);
    }
}
