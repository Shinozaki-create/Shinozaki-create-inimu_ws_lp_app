package com.example.inimuws.controller.admin;

import com.example.inimuws.dto.DashboardCalendarDay;
import com.example.inimuws.dto.DashboardTrend;
import com.example.inimuws.dto.MonthlySalesSummary;
import com.example.inimuws.entity.Reservation;
import com.example.inimuws.entity.WorkshopSchedule;
import com.example.inimuws.entity.WorkshopTimeSlot;
import com.example.inimuws.service.InquiryService;
import com.example.inimuws.service.ReservationService;
import com.example.inimuws.service.SalesService;
import com.example.inimuws.service.ScheduleService;
import java.text.NumberFormat;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月");
    private static final DateTimeFormatter MONTH_QUERY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReservationService reservationService;
    private final InquiryService inquiryService;
    private final SalesService salesService;
    private final ScheduleService scheduleService;

    @GetMapping("/admin")
    public String dashboard(@RequestParam(required = false) String month, Model model) {
        YearMonth targetMonth = parseMonth(month);
        YearMonth previousMonth = targetMonth.minusMonths(1);
        MonthlySalesSummary monthSales = salesService.getMonthlySummary(targetMonth);
        MonthlySalesSummary previousMonthSales = salesService.getMonthlySummary(previousMonth);
        long monthReservationCount = reservationService.countReservationsForMonth(targetMonth);
        long previousMonthReservationCount = reservationService.countReservationsForMonth(previousMonth);
        long monthParticipantCount = monthSales.participantCount();
        long previousMonthParticipantCount = previousMonthSales.participantCount();
        List<Reservation> monthReservations = reservationService.findReservationsForMonth(targetMonth, 8);
        List<WorkshopSchedule> monthSchedules = scheduleService.findSchedulesForMonth(targetMonth);

        model.addAttribute("targetMonthLabel", targetMonth.format(MONTH_LABEL_FORMATTER));
        model.addAttribute("targetMonthQuery", targetMonth.format(MONTH_QUERY_FORMATTER));
        model.addAttribute("prevMonthQuery", previousMonth.format(MONTH_QUERY_FORMATTER));
        model.addAttribute("nextMonthQuery", targetMonth.plusMonths(1).format(MONTH_QUERY_FORMATTER));
        model.addAttribute("monthReservationCount", monthReservationCount);
        model.addAttribute("monthReservationTrend", buildTrend(monthReservationCount, previousMonthReservationCount));
        model.addAttribute("monthParticipantCount", monthParticipantCount);
        model.addAttribute("monthParticipantTrend", buildTrend(monthParticipantCount, previousMonthParticipantCount));
        model.addAttribute("monthSales", monthSales);
        model.addAttribute("monthSalesDisplay", formatCurrency(monthSales.totalAmount()));
        model.addAttribute("monthSalesTrend", buildTrend(monthSales.totalAmount(), previousMonthSales.totalAmount()));
        model.addAttribute("openInquiryCount", inquiryService.countOpen());
        model.addAttribute("inProgressInquiryCount", inquiryService.countInProgress());
        model.addAttribute("monthReservations", monthReservations);
        model.addAttribute("calendarWeeks", buildCalendarWeeks(targetMonth, monthSchedules));
        return "admin/dashboard";
    }

    private YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            return YearMonth.now();
        }
        return YearMonth.parse(month);
    }

    private DashboardTrend buildTrend(long current, long previous) {
        if (previous <= 0) {
            if (current <= 0) {
                return new DashboardTrend("前月比 0%", "trend-neutral");
            }
            return new DashboardTrend("前月比 -", "trend-neutral");
        }

        long percent = Math.round(((double) (current - previous) / (double) previous) * 100.0d);
        String sign = percent > 0 ? "+" : "";
        String cssClass = percent > 0 ? "trend-positive" : percent < 0 ? "trend-negative" : "trend-neutral";
        return new DashboardTrend("前月比 " + sign + percent + "%", cssClass);
    }

    private String formatCurrency(int amount) {
        return "¥" + NumberFormat.getNumberInstance(Locale.JAPAN).format(amount);
    }

    private List<List<DashboardCalendarDay>> buildCalendarWeeks(YearMonth targetMonth, List<WorkshopSchedule> schedules) {
        Map<LocalDate, WorkshopSchedule> scheduleMap = schedules.stream()
                .collect(Collectors.toMap(WorkshopSchedule::getScheduleDate, Function.identity()));
        Map<LocalDate, Integer> reservedCountMap = schedules.stream()
                .collect(Collectors.toMap(
                        WorkshopSchedule::getScheduleDate,
                        schedule -> schedule.getTimeSlots().stream()
                                .mapToInt(WorkshopTimeSlot::getReservedCount)
                                .sum()
                ));

        LocalDate firstDay = targetMonth.atDay(1);
        LocalDate lastDay = targetMonth.atEndOfMonth();
        LocalDate start = firstDay.minusDays(firstDay.getDayOfWeek().getValue() % 7L);
        LocalDate end = lastDay.plusDays(6L - (lastDay.getDayOfWeek().getValue() % 7L));

        List<List<DashboardCalendarDay>> weeks = new ArrayList<>();
        List<DashboardCalendarDay> currentWeek = new ArrayList<>(7);

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            WorkshopSchedule schedule = scheduleMap.get(day);
            currentWeek.add(new DashboardCalendarDay(
                    day,
                    day.getMonth() == targetMonth.getMonth(),
                    schedule != null,
                    schedule != null && schedule.isOpen(),
                    reservedCountMap.getOrDefault(day, 0),
                    schedule == null ? "" : (schedule.isOpen() ? "開催" : "休止")
            ));

            if (day.getDayOfWeek() == DayOfWeek.SATURDAY) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>(7);
            }
        }

        return weeks;
    }
}
