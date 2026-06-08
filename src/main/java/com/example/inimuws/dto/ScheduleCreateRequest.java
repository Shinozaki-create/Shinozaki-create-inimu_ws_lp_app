package com.example.inimuws.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class ScheduleCreateRequest {

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(0)
    private int capacity = 10;

    private boolean open = true;

    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;
    private boolean sunday;
    private boolean holiday;

    private boolean slot1 = true;
    private boolean slot2 = true;
    private boolean slot3 = true;

    private String note;

    @AssertTrue(message = "終了日は開始日以降を指定してください")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "少なくとも1つの曜日または祝日を選択してください")
    public boolean isTargetDaySelected() {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday || holiday;
    }

    @AssertTrue(message = "少なくとも1つの時間枠を選択してください")
    public boolean isTargetTimeSlotSelected() {
        return slot1 || slot2 || slot3;
    }

    public List<DayOfWeek> selectedWeekdays() {
        List<DayOfWeek> weekdays = new ArrayList<>();
        if (monday) {
            weekdays.add(DayOfWeek.MONDAY);
        }
        if (tuesday) {
            weekdays.add(DayOfWeek.TUESDAY);
        }
        if (wednesday) {
            weekdays.add(DayOfWeek.WEDNESDAY);
        }
        if (thursday) {
            weekdays.add(DayOfWeek.THURSDAY);
        }
        if (friday) {
            weekdays.add(DayOfWeek.FRIDAY);
        }
        if (saturday) {
            weekdays.add(DayOfWeek.SATURDAY);
        }
        if (sunday) {
            weekdays.add(DayOfWeek.SUNDAY);
        }
        return weekdays;
    }

    public List<Integer> selectedTimeSlotIndexes() {
        List<Integer> indexes = new ArrayList<>();
        if (slot1) {
            indexes.add(0);
        }
        if (slot2) {
            indexes.add(1);
        }
        if (slot3) {
            indexes.add(2);
        }
        return indexes;
    }
}
