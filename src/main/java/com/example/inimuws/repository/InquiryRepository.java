package com.example.inimuws.repository;

import com.example.inimuws.entity.Inquiry;
import com.example.inimuws.enums.InquiryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    long countByStatus(InquiryStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime endExclusive);

    boolean existsByInquiryCode(String inquiryCode);

    List<Inquiry> findAllByOrderByCreatedAtDesc();
}
