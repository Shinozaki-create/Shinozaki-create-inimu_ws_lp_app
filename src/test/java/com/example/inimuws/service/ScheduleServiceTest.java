package com.example.inimuws.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.inimuws.dto.ScheduleCreateRequest;
import com.example.inimuws.entity.WorkshopSchedule;
import com.example.inimuws.entity.WorkshopTimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Test
    void createsMultipleSchedulesWithThreeFixedTimeSlots() {
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setStartDate(LocalDate.of(2026, 10, 1));
        request.setEndDate(LocalDate.of(2026, 10, 7));
        request.setMonday(true);
        request.setTuesday(true);
        request.setWednesday(true);
        request.setThursday(true);
        request.setFriday(true);
        request.setSaturday(true);
        request.setSunday(true);
        request.setSlot1(true);
        request.setSlot2(true);
        request.setSlot3(true);
        request.setCapacity(12);
        request.setOpen(true);
        request.setNote("秋の開催日");

        List<WorkshopSchedule> createdSchedules = scheduleService.createSchedule(request);

        assertThat(createdSchedules).hasSize(7);
        assertThat(createdSchedules)
                .allSatisfy(schedule -> {
                    assertThat(schedule.getTimeSlots()).hasSize(3);
                    assertThat(schedule.getTimeSlots())
                            .extracting(WorkshopTimeSlot::getStartTime)
                            .containsExactly(LocalTime.of(11, 0), LocalTime.of(13, 0), LocalTime.of(15, 0));
                    assertThat(schedule.getTimeSlots())
                            .extracting(WorkshopTimeSlot::getCapacity)
                            .containsOnly(12);
                });
    }

    @Test
    void createsHolidayOnlySchedulesWhenHolidayIsSelected() {
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setStartDate(LocalDate.of(2026, 11, 3));
        request.setEndDate(LocalDate.of(2026, 11, 3));
        request.setHoliday(true);
        request.setSlot1(true);
        request.setSlot2(false);
        request.setSlot3(true);
        request.setCapacity(8);
        request.setOpen(true);
        request.setNote("文化の日");

        List<WorkshopSchedule> createdSchedules = scheduleService.createSchedule(request);

        assertThat(createdSchedules).hasSize(1);
        assertThat(createdSchedules.get(0).getScheduleDate()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(createdSchedules.get(0).getTimeSlots())
                .hasSize(2);
        assertThat(createdSchedules.get(0).getTimeSlots())
                .extracting(WorkshopTimeSlot::getStartTime)
                .containsExactly(LocalTime.of(11, 0), LocalTime.of(15, 0));
        assertThat(createdSchedules.get(0).getTimeSlots())
                .extracting(WorkshopTimeSlot::getCapacity)
                .containsOnly(8);
    }

    @Test
    void createsOnlySelectedTimeSlots() {
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setStartDate(LocalDate.of(2026, 10, 5));
        request.setEndDate(LocalDate.of(2026, 10, 5));
        request.setMonday(true);
        request.setSlot1(true);
        request.setSlot2(false);
        request.setSlot3(true);
        request.setCapacity(9);
        request.setOpen(true);
        request.setNote("一部時間枠のみ");

        List<WorkshopSchedule> createdSchedules = scheduleService.createSchedule(request);

        assertThat(createdSchedules).hasSize(1);
        assertThat(createdSchedules.get(0).getTimeSlots())
                .hasSize(2)
                .extracting(WorkshopTimeSlot::getStartTime)
                .containsExactly(LocalTime.of(11, 0), LocalTime.of(15, 0));
    }
}
