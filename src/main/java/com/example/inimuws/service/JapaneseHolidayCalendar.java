package com.example.inimuws.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class JapaneseHolidayCalendar {

    private final ConcurrentMap<Integer, Set<LocalDate>> holidayCache = new ConcurrentHashMap<>();

    public boolean isHoliday(LocalDate date) {
        return holidayCache.computeIfAbsent(date.getYear(), this::buildHolidaySet).contains(date);
    }

    private Set<LocalDate> buildHolidaySet(int year) {
        Set<LocalDate> holidays = new LinkedHashSet<>();
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            if (isBaseHoliday(day)) {
                holidays.add(day);
            }
        }

        addSubstituteHolidays(holidays, year);
        addCitizenHolidays(holidays, year);
        return Set.copyOf(holidays);
    }

    private boolean isBaseHoliday(LocalDate date) {
        return switch (date.getMonth()) {
            case JANUARY -> date.getDayOfMonth() == 1
                    || date.equals(nthMonday(date, 2));
            case FEBRUARY -> date.getDayOfMonth() == 11
                    || (date.getYear() >= 2020 && date.getDayOfMonth() == 23);
            case MARCH -> date.getDayOfMonth() == vernalEquinoxDay(date.getYear());
            case APRIL -> date.getDayOfMonth() == 29;
            case MAY -> date.getDayOfMonth() == 3
                    || date.getDayOfMonth() == 4
                    || date.getDayOfMonth() == 5;
            case JULY -> date.equals(nthMonday(date, 3));
            case AUGUST -> date.getDayOfMonth() == 11;
            case SEPTEMBER -> date.equals(nthMonday(date, 3))
                    || date.getDayOfMonth() == autumnEquinoxDay(date.getYear());
            case OCTOBER -> date.equals(nthMonday(date, 2));
            case NOVEMBER -> date.getDayOfMonth() == 3
                    || date.getDayOfMonth() == 23;
            case DECEMBER -> date.getYear() >= 1989 && date.getYear() <= 2018 && date.getDayOfMonth() == 23;
            default -> false;
        };
    }

    private void addSubstituteHolidays(Set<LocalDate> holidays, int year) {
        List<LocalDate> currentHolidays = new ArrayList<>(holidays);
        for (LocalDate holiday : currentHolidays) {
            if (holiday.getDayOfWeek() != DayOfWeek.SUNDAY) {
                continue;
            }
            LocalDate substitute = holiday.plusDays(1);
            while (substitute.getYear() == year && holidays.contains(substitute)) {
                substitute = substitute.plusDays(1);
            }
            if (substitute.getYear() == year) {
                holidays.add(substitute);
            }
        }
    }

    private void addCitizenHolidays(Set<LocalDate> holidays, int year) {
        boolean changed;
        do {
            changed = false;
            for (LocalDate day = LocalDate.of(year, 1, 2); day.isBefore(LocalDate.of(year, 12, 31)); day = day.plusDays(1)) {
                if (holidays.contains(day)) {
                    continue;
                }
                if (holidays.contains(day.minusDays(1)) && holidays.contains(day.plusDays(1))) {
                    holidays.add(day);
                    changed = true;
                }
            }
        } while (changed);
    }

    private LocalDate nthMonday(LocalDate date, int nth) {
        return LocalDate.of(date.getYear(), date.getMonth(), 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(nth, DayOfWeek.MONDAY));
    }

    private int vernalEquinoxDay(int year) {
        if (year <= 1979) {
            return (int) Math.floor(20.8357 + 0.242194 * (year - 1980) - Math.floor((year - 1983) / 4.0d));
        }
        if (year <= 2099) {
            return (int) Math.floor(20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0d));
        }
        return (int) Math.floor(21.8510 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0d));
    }

    private int autumnEquinoxDay(int year) {
        if (year <= 1979) {
            return (int) Math.floor(23.2588 + 0.242194 * (year - 1980) - Math.floor((year - 1983) / 4.0d));
        }
        if (year <= 2099) {
            return (int) Math.floor(23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0d));
        }
        return (int) Math.floor(24.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4.0d));
    }
}
