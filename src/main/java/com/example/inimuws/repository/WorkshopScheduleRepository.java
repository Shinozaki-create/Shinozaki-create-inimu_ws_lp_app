package com.example.inimuws.repository;

import com.example.inimuws.entity.WorkshopSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopScheduleRepository extends JpaRepository<WorkshopSchedule, Long> {

    Optional<WorkshopSchedule> findByScheduleDate(LocalDate scheduleDate);

    @EntityGraph(attributePaths = "timeSlots")
    List<WorkshopSchedule> findAllByOrderByScheduleDateAsc();

    @EntityGraph(attributePaths = "timeSlots")
    List<WorkshopSchedule> findAllByScheduleDateBetweenOrderByScheduleDateAsc(LocalDate start, LocalDate end);
}
