package com.example.inimuws.service;

import com.example.inimuws.dto.ScheduleCreateRequest;
import com.example.inimuws.dto.ScheduleResponse;
import com.example.inimuws.dto.ScheduleUpdateRequest;
import com.example.inimuws.dto.TimeSlotCreateRequest;
import com.example.inimuws.dto.TimeSlotResponse;
import com.example.inimuws.dto.TimeSlotUpdateRequest;
import com.example.inimuws.entity.WorkshopSchedule;
import com.example.inimuws.entity.WorkshopTimeSlot;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.repository.WorkshopScheduleRepository;
import com.example.inimuws.repository.WorkshopTimeSlotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final List<TimeSlotTemplate> DEFAULT_TIME_SLOTS = List.of(
            new TimeSlotTemplate(LocalTime.of(11, 0), LocalTime.of(12, 0)),
            new TimeSlotTemplate(LocalTime.of(13, 0), LocalTime.of(14, 0)),
            new TimeSlotTemplate(LocalTime.of(15, 0), LocalTime.of(16, 0))
    );

    private final WorkshopScheduleRepository scheduleRepository;
    private final WorkshopTimeSlotRepository timeSlotRepository;
    private final JapaneseHolidayCalendar holidayCalendar;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getPublicSchedules() {
        return scheduleRepository.findAllByOrderByScheduleDateAsc().stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getPublicTimeSlots(LocalDate date) {
        return timeSlotRepository.findBySchedule_ScheduleDateOrderByStartTimeAsc(date).stream()
                .map(this::toTimeSlotResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkshopSchedule> findAllSchedules() {
        return scheduleRepository.findAllByOrderByScheduleDateAsc();
    }

    @Transactional(readOnly = true)
    public List<WorkshopSchedule> findSchedulesForMonth(YearMonth month) {
        return scheduleRepository.findAllByScheduleDateBetweenOrderByScheduleDateAsc(
                month.atDay(1),
                month.atEndOfMonth()
        );
    }

    @Transactional(readOnly = true)
    public List<WorkshopTimeSlot> findSlots(Long scheduleId) {
        return timeSlotRepository.findByScheduleIdOrderByStartTimeAsc(scheduleId);
    }

    @Transactional(readOnly = true)
    public List<WorkshopTimeSlot> findLowAvailabilitySlots(int threshold) {
        return timeSlotRepository.findLowAvailabilitySlots(threshold);
    }

    @Transactional
    public List<WorkshopSchedule> createSchedule(ScheduleCreateRequest request) {
        validateScheduleCreateRequest(request);

        List<LocalDate> targetDates = collectTargetDates(request);
        if (targetDates.isEmpty()) {
            throw new BusinessException("指定条件に一致する開催日がありません");
        }

        for (LocalDate targetDate : targetDates) {
            if (scheduleRepository.findByScheduleDate(targetDate).isPresent()) {
                throw new BusinessException("指定期間に既に開催日が登録されています: " + targetDate);
            }
        }

        List<WorkshopSchedule> createdSchedules = new ArrayList<>();
        for (LocalDate targetDate : targetDates) {
            WorkshopSchedule schedule = buildSchedule(targetDate, request);
            createdSchedules.add(scheduleRepository.save(schedule));
        }
        return createdSchedules;
    }

    @Transactional
    public WorkshopSchedule updateSchedule(Long id, ScheduleUpdateRequest request) {
        WorkshopSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("開催日が見つかりません"));
        schedule.setOpen(Boolean.TRUE.equals(request.getOpen()));
        schedule.setNote(request.getNote());
        return schedule;
    }

    @Transactional
    public WorkshopTimeSlot createTimeSlot(TimeSlotCreateRequest request) {
        WorkshopSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> BusinessException.notFound("開催日が見つかりません"));
        validateTimeRange(request.getStartTime(), request.getEndTime());
        WorkshopTimeSlot slot = WorkshopTimeSlot.builder()
                .schedule(schedule)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .reservedCount(0)
                .active(request.isActive())
                .build();
        return timeSlotRepository.save(slot);
    }

    @Transactional
    public WorkshopTimeSlot updateTimeSlot(Long id, TimeSlotUpdateRequest request) {
        WorkshopTimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("時間枠が見つかりません"));
        if (request.getCapacity() < slot.getReservedCount()) {
            throw new BusinessException("予約済み人数より少ない定員には変更できません");
        }
        slot.setCapacity(request.getCapacity());
        slot.setActive(request.isActive());
        return slot;
    }

    public ScheduleResponse toScheduleResponse(WorkshopSchedule schedule) {
        List<WorkshopTimeSlot> activeSlots = schedule.getTimeSlots().stream()
                .filter(WorkshopTimeSlot::isActive)
                .toList();
        int totalCapacity = activeSlots.stream().mapToInt(WorkshopTimeSlot::getCapacity).sum();
        int reservedCount = activeSlots.stream().mapToInt(WorkshopTimeSlot::getReservedCount).sum();
        int remainingCount = Math.max(0, totalCapacity - reservedCount);
        boolean fullyBooked = schedule.isOpen() && totalCapacity > 0 && remainingCount == 0;
        return new ScheduleResponse(
                schedule.getScheduleDate(),
                schedule.isOpen(),
                totalCapacity,
                reservedCount,
                remainingCount,
                fullyBooked
        );
    }

    public TimeSlotResponse toTimeSlotResponse(WorkshopTimeSlot slot) {
        return new TimeSlotResponse(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getReservedCount(),
                slot.remainingCount(),
                slot.isActive(),
                slot.isFullyBooked()
        );
    }

    private void validateScheduleCreateRequest(ScheduleCreateRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException("開始日と終了日を指定してください");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("終了日は開始日以降を指定してください");
        }
        if (request.selectedWeekdays().isEmpty() && !request.isHoliday()) {
            throw new BusinessException("少なくとも1つの曜日または祝日を選択してください");
        }
        if (request.selectedTimeSlotIndexes().isEmpty()) {
            throw new BusinessException("少なくとも1つの時間枠を選択してください");
        }
    }

    private List<LocalDate> collectTargetDates(ScheduleCreateRequest request) {
        List<DayOfWeek> selectedWeekdays = request.selectedWeekdays();
        List<LocalDate> targetDates = new ArrayList<>();
        for (LocalDate date = request.getStartDate(); !date.isAfter(request.getEndDate()); date = date.plusDays(1)) {
            boolean matchesWeekday = selectedWeekdays.contains(date.getDayOfWeek());
            boolean matchesHoliday = request.isHoliday() && holidayCalendar.isHoliday(date);
            if (matchesWeekday || matchesHoliday) {
                targetDates.add(date);
            }
        }
        return targetDates;
    }

    private WorkshopSchedule buildSchedule(LocalDate scheduleDate, ScheduleCreateRequest request) {
        WorkshopSchedule schedule = WorkshopSchedule.builder()
                .scheduleDate(scheduleDate)
                .open(request.isOpen())
                .note(request.getNote())
                .build();

        List<TimeSlotTemplate> selectedTimeSlots = selectedTimeSlots(request);
        for (TimeSlotTemplate template : selectedTimeSlots) {
            WorkshopTimeSlot slot = WorkshopTimeSlot.builder()
                    .schedule(schedule)
                    .startTime(template.startTime())
                    .endTime(template.endTime())
                    .capacity(request.getCapacity())
                    .reservedCount(0)
                    .active(true)
                    .build();
            schedule.getTimeSlots().add(slot);
        }
        return schedule;
    }

    private List<TimeSlotTemplate> selectedTimeSlots(ScheduleCreateRequest request) {
        List<TimeSlotTemplate> selectedTimeSlots = new ArrayList<>();
        List<Integer> selectedIndexes = request.selectedTimeSlotIndexes();
        for (int index = 0; index < DEFAULT_TIME_SLOTS.size(); index++) {
            if (selectedIndexes.contains(index)) {
                selectedTimeSlots.add(DEFAULT_TIME_SLOTS.get(index));
            }
        }
        return selectedTimeSlots;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("終了時刻は開始時刻より後にしてください");
        }
    }

    private record TimeSlotTemplate(LocalTime startTime, LocalTime endTime) {
    }
}
