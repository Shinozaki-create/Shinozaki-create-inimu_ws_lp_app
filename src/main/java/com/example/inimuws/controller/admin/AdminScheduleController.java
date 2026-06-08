package com.example.inimuws.controller.admin;

import com.example.inimuws.dto.ScheduleCalendarDay;
import com.example.inimuws.dto.ScheduleCreateRequest;
import com.example.inimuws.dto.ScheduleUpdateRequest;
import com.example.inimuws.dto.TimeSlotCreateRequest;
import com.example.inimuws.dto.TimeSlotResponse;
import com.example.inimuws.dto.TimeSlotUpdateRequest;
import com.example.inimuws.entity.WorkshopSchedule;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月", Locale.JAPAN);
    private static final DateTimeFormatter MONTH_QUERY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ScheduleService scheduleService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String month,
            Model model
    ) {
        YearMonth targetMonth = parseMonth(month);
        populateModel(model, targetMonth, createDefaultScheduleCreateRequest(targetMonth));
        return "admin/schedules";
    }

    @PostMapping
    public String createSchedule(
            @Valid @ModelAttribute("scheduleCreateRequest") ScheduleCreateRequest request,
            BindingResult bindingResult,
            @RequestParam(required = false) String month,
            Model model
    ) {
        YearMonth targetMonth = parseMonth(month);
        if (bindingResult.hasErrors()) {
            populateModel(model, targetMonth, request);
            return "admin/schedules";
        }

        try {
            scheduleService.createSchedule(request);
            return redirectToMonth(targetMonth);
        } catch (BusinessException exception) {
            populateModel(model, targetMonth, request);
            model.addAttribute("scheduleError", exception.getMessage());
            return "admin/schedules";
        }
    }

    @PostMapping("/{id}")
    public String updateSchedule(
            @PathVariable Long id,
            @Valid @ModelAttribute ScheduleUpdateRequest request,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scheduleService.updateSchedule(id, request);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("scheduleError", exception.getMessage());
        }
        return redirectToMonth(parseMonth(month));
    }

    @PostMapping("/slots")
    public String createTimeSlot(
            @Valid @ModelAttribute TimeSlotCreateRequest request,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scheduleService.createTimeSlot(request);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("scheduleError", exception.getMessage());
        }
        return redirectToMonth(parseMonth(month));
    }

    @PostMapping("/slots/{id}")
    public String updateTimeSlot(
            @PathVariable Long id,
            @Valid @ModelAttribute TimeSlotUpdateRequest request,
            @RequestParam(required = false) String month,
            RedirectAttributes redirectAttributes
    ) {
        try {
            scheduleService.updateTimeSlot(id, request);
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("scheduleError", exception.getMessage());
        }
        return redirectToMonth(parseMonth(month));
    }

    private void populateModel(Model model, YearMonth targetMonth, ScheduleCreateRequest request) {
        List<WorkshopSchedule> schedules = scheduleService.findSchedulesForMonth(targetMonth);
        model.addAttribute("schedules", schedules);
        model.addAttribute("scheduleCreateRequest", request);
        model.addAttribute("targetMonthLabel", targetMonth.format(MONTH_LABEL_FORMATTER));
        model.addAttribute("targetMonthQuery", targetMonth.format(MONTH_QUERY_FORMATTER));
        model.addAttribute("prevMonthQuery", targetMonth.minusMonths(1).format(MONTH_QUERY_FORMATTER));
        model.addAttribute("nextMonthQuery", targetMonth.plusMonths(1).format(MONTH_QUERY_FORMATTER));
        model.addAttribute("calendarWeeks", buildCalendarWeeks(targetMonth, schedules));
    }

    private ScheduleCreateRequest createDefaultScheduleCreateRequest(YearMonth targetMonth) {
        ScheduleCreateRequest request = new ScheduleCreateRequest();
        request.setStartDate(targetMonth.atDay(1));
        request.setEndDate(targetMonth.atEndOfMonth());
        request.setSlot1(true);
        request.setSlot2(true);
        request.setSlot3(true);
        return request;
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }

    private String redirectToMonth(YearMonth month) {
        String query = month.format(MONTH_QUERY_FORMATTER);
        return "redirect:/admin/schedules?month=" + query;
    }

    private List<List<ScheduleCalendarDay>> buildCalendarWeeks(YearMonth targetMonth, List<WorkshopSchedule> schedules) {
        Map<LocalDate, WorkshopSchedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(WorkshopSchedule::getScheduleDate, Function.identity()));
        Map<LocalDate, List<TimeSlotResponse>> slotMap = schedules.stream()
                .collect(Collectors.toMap(
                        WorkshopSchedule::getScheduleDate,
                        schedule -> schedule.getTimeSlots().stream()
                                .map(scheduleService::toTimeSlotResponse)
                                .toList()
                ));

        LocalDate firstDay = targetMonth.atDay(1);
        LocalDate lastDay = targetMonth.atEndOfMonth();
        LocalDate start = firstDay.minusDays(firstDay.getDayOfWeek().getValue() % 7L);
        LocalDate end = lastDay.plusDays(6L - (lastDay.getDayOfWeek().getValue() % 7L));

        List<List<ScheduleCalendarDay>> weeks = new ArrayList<>();
        List<ScheduleCalendarDay> currentWeek = new ArrayList<>(7);

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            WorkshopSchedule schedule = scheduleMap.get(day);
            List<TimeSlotResponse> timeSlots = slotMap.getOrDefault(day, List.of());
            boolean fullyBooked = schedule != null && scheduleService.toScheduleResponse(schedule).fullyBooked();
            currentWeek.add(new ScheduleCalendarDay(
                    day,
                    day.getMonth() == targetMonth.getMonth(),
                    schedule != null,
                    schedule != null && schedule.isOpen(),
                    fullyBooked,
                    timeSlots
            ));

            if (day.getDayOfWeek() == DayOfWeek.SATURDAY) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>(7);
            }
        }

        return weeks;
    }
}
