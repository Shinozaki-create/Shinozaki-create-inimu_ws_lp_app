package com.example.inimuws.repository;

import com.example.inimuws.entity.Reservation;
import com.example.inimuws.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    long countByReservationDate(LocalDate reservationDate);

    long countByReservationDateBetween(LocalDate start, LocalDate end);

    long countByStatus(ReservationStatus status);

    boolean existsByReservationCode(String reservationCode);

    Optional<Reservation> findByReservationCode(String reservationCode);

    @EntityGraph(attributePaths = "timeSlot")
    List<Reservation> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "timeSlot")
    List<Reservation> findByReservationDateBetweenOrderByReservationDateAscReservationTimeAsc(LocalDate start, LocalDate end);

    @Query("""
            select coalesce(sum(r.totalAmount), 0)
            from Reservation r
            where r.status in :statuses and r.reservationDate between :start and :end
            """)
    int sumTotalAmountByStatusesAndReservationDateBetween(
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
            select coalesce(sum(coalesce(r.participantCount, r.reservationCount)), 0)
            from Reservation r
            where r.status in :statuses and r.reservationDate between :start and :end
            """)
    int sumParticipantCountByStatusesAndReservationDateBetween(
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    long countByStatusInAndReservationDateBetween(Collection<ReservationStatus> statuses, LocalDate start, LocalDate end);
}
