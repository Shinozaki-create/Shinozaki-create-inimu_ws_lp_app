package com.example.inimuws.service;

import com.example.inimuws.dto.DailySalesSummary;
import com.example.inimuws.dto.MonthlySalesSummary;
import com.example.inimuws.entity.Reservation;
import com.example.inimuws.enums.ReservationStatus;
import com.example.inimuws.repository.ReservationRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesService {

    private static final EnumSet<ReservationStatus> BILLABLE_STATUSES =
            EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);

    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public MonthlySalesSummary getMonthlySummary(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        int totalAmount = reservationRepository.sumTotalAmountByStatusesAndReservationDateBetween(BILLABLE_STATUSES, start, end);
        int participantCount = reservationRepository.sumParticipantCountByStatusesAndReservationDateBetween(BILLABLE_STATUSES, start, end);
        long reservationCount = reservationRepository.countByStatusInAndReservationDateBetween(BILLABLE_STATUSES, start, end);
        return new MonthlySalesSummary(month, totalAmount, reservationCount, participantCount);
    }

    @Transactional(readOnly = true)
    public List<DailySalesSummary> getDailySummaries(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Map<LocalDate, List<Reservation>> grouped = reservationRepository
                .findByReservationDateBetweenOrderByReservationDateAscReservationTimeAsc(start, end)
                .stream()
                .filter(reservation -> BILLABLE_STATUSES.contains(reservation.getStatus()))
                .collect(Collectors.groupingBy(Reservation::getReservationDate));

        return grouped.entrySet().stream()
                .map(entry -> new DailySalesSummary(
                        entry.getKey(),
                        entry.getValue().stream().mapToInt(Reservation::getTotalAmount).sum(),
                        entry.getValue().size(),
                        entry.getValue().stream().mapToInt(Reservation::effectiveParticipantCount).sum()
                ))
                .sorted(Comparator.comparing(DailySalesSummary::date))
                .toList();
    }
}
