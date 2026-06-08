package com.example.inimuws.repository;

import com.example.inimuws.entity.WorkshopTimeSlot;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkshopTimeSlotRepository extends JpaRepository<WorkshopTimeSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WorkshopTimeSlot t join fetch t.schedule where t.id = :id")
    Optional<WorkshopTimeSlot> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from WorkshopTimeSlot t
            join fetch t.schedule s
            where s.scheduleDate = :date and t.startTime = :startTime
            """)
    Optional<WorkshopTimeSlot> findByScheduleDateAndStartTimeForUpdate(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime
    );

    @EntityGraph(attributePaths = "schedule")
    List<WorkshopTimeSlot> findByScheduleIdOrderByStartTimeAsc(Long scheduleId);

    @EntityGraph(attributePaths = "schedule")
    List<WorkshopTimeSlot> findBySchedule_ScheduleDateOrderByStartTimeAsc(LocalDate scheduleDate);

    @Query("""
            select t
            from WorkshopTimeSlot t
            join fetch t.schedule s
            where s.open = true and t.active = true and (t.capacity - t.reservedCount) <= :threshold
            order by s.scheduleDate asc, t.startTime asc
            """)
    List<WorkshopTimeSlot> findLowAvailabilitySlots(@Param("threshold") int threshold);
}
