package com.example.inimuws.service;

import com.example.inimuws.dto.AdminReservationSearchCondition;
import com.example.inimuws.dto.ReservationRequest;
import com.example.inimuws.dto.ReservationResponse;
import com.example.inimuws.dto.ReservationStatusUpdateRequest;
import com.example.inimuws.entity.Reservation;
import com.example.inimuws.entity.SystemSetting;
import com.example.inimuws.entity.WorkshopSchedule;
import com.example.inimuws.entity.WorkshopTimeSlot;
import com.example.inimuws.enums.ReservationStatus;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.repository.ReservationRepository;
import com.example.inimuws.repository.SystemSettingRepository;
import com.example.inimuws.repository.WorkshopTimeSlotRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String PRICE_PER_PERSON_KEY = "price_per_person";
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final ReservationRepository reservationRepository;
    private final WorkshopTimeSlotRepository timeSlotRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final InquiryService inquiryService;

    @Transactional
    public ReservationResponse createReservationOrInquiry(ReservationRequest request) {
        validateCommon(request);
        if (request.isInquiryOnly()) {
            inquiryService.createInquiry(request);
            return ReservationResponse.inquiry();
        }

        Reservation reservation = createReservation(request, ReservationStatus.PENDING);
        return ReservationResponse.reservation(reservation.getReservationCode());
    }

    @Transactional
    public Reservation createAdminReservation(ReservationRequest request) {
        validateCommon(request);
        return createReservation(request, ReservationStatus.CONFIRMED);
    }

    @Transactional(readOnly = true)
    public String previewReservationCode(LocalDate reservationDate) {
        return generateReservationCode(reservationDate);
    }

    @Transactional(readOnly = true)
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("莠育ｴ・′隕九▽縺九ｊ縺ｾ縺帙ｓ"));
    }

    @Transactional(readOnly = true)
    public List<Reservation> searchReservations(AdminReservationSearchCondition condition) {
        return reservationRepository.findAll(buildSpecification(condition), buildSort(condition));
    }

    @Transactional
    public Reservation updateStatus(Long id, ReservationStatusUpdateRequest request) {
        Reservation reservation = findById(id);
        ReservationStatus before = reservation.getStatus();
        ReservationStatus after = request.getStatus();
        int pricePerPerson = getPricePerPerson();
        boolean hasAgeBreakdown = hasAgeBreakdown(request);

        int participantCount;
        int maleCount;
        int femaleCount;
        if (hasAgeBreakdown) {
            maleCount = sum(
                    request.getMaleUnder10Count(),
                    request.getMale20sCount(),
                    request.getMale30sCount(),
                    request.getMale40sCount(),
                    request.getMale50sCount(),
                    request.getMale60PlusCount()
            );
            femaleCount = sum(
                    request.getFemaleUnder10Count(),
                    request.getFemale20sCount(),
                    request.getFemale30sCount(),
                    request.getFemale40sCount(),
                    request.getFemale50sCount(),
                    request.getFemale60PlusCount()
            );
            participantCount = maleCount + femaleCount;
        } else {
            participantCount = request.getParticipantCount() != null
                    ? request.getParticipantCount()
                    : reservation.effectiveParticipantCount();
            maleCount = request.getMaleCount() != null ? request.getMaleCount() : reservation.effectiveMaleCount();
            femaleCount = request.getFemaleCount() != null ? request.getFemaleCount() : reservation.effectiveFemaleCount();
        }

        if (before != ReservationStatus.CANCELLED && after == ReservationStatus.CANCELLED) {
            WorkshopTimeSlot slot = timeSlotRepository.findByIdForUpdate(reservation.getTimeSlot().getId())
                    .orElseThrow(() -> BusinessException.notFound("Time slot not found"));
            slot.setReservedCount(Math.max(0, slot.getReservedCount() - reservation.getReservationCount()));
        } else if (before == ReservationStatus.CANCELLED && after != ReservationStatus.CANCELLED) {
            WorkshopTimeSlot slot = timeSlotRepository.findByIdForUpdate(reservation.getTimeSlot().getId())
                    .orElseThrow(() -> BusinessException.notFound("Time slot not found"));
            if (slot.getReservedCount() + reservation.getReservationCount() > slot.getCapacity()) {
                throw new BusinessException("Not enough capacity");
            }
            slot.setReservedCount(slot.getReservedCount() + reservation.getReservationCount());
        }

        reservation.setStatus(after);
        reservation.setParticipantCount(participantCount);
        reservation.setMaleCount(maleCount);
        reservation.setFemaleCount(femaleCount);
        if (hasAgeBreakdown) {
            reservation.setMaleUnder10Count(normalize(request.getMaleUnder10Count()));
            reservation.setMale20sCount(normalize(request.getMale20sCount()));
            reservation.setMale30sCount(normalize(request.getMale30sCount()));
            reservation.setMale40sCount(normalize(request.getMale40sCount()));
            reservation.setMale50sCount(normalize(request.getMale50sCount()));
            reservation.setMale60PlusCount(normalize(request.getMale60PlusCount()));
            reservation.setFemaleUnder10Count(normalize(request.getFemaleUnder10Count()));
            reservation.setFemale20sCount(normalize(request.getFemale20sCount()));
            reservation.setFemale30sCount(normalize(request.getFemale30sCount()));
            reservation.setFemale40sCount(normalize(request.getFemale40sCount()));
            reservation.setFemale50sCount(normalize(request.getFemale50sCount()));
            reservation.setFemale60PlusCount(normalize(request.getFemale60PlusCount()));
        }
        reservation.setTotalAmount(participantCount * pricePerPerson);
        reservation.setAdminMemo(request.getAdminMemo());
        return reservation;
    }

    @Transactional(readOnly = true)
    public long countTodayReservations() {
        return reservationRepository.countByReservationDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long countThisMonthReservations() {
        YearMonth month = YearMonth.now();
        return reservationRepository.countByReservationDateBetween(month.atDay(1), month.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public long countReservationsForMonth(YearMonth month) {
        return reservationRepository.countByReservationDateBetween(month.atDay(1), month.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reservationRepository.countByStatus(ReservationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findRecentReservations() {
        return reservationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Reservation> findReservationsForMonth(YearMonth month, int limit) {
        return reservationRepository.findByReservationDateBetweenOrderByReservationDateAscReservationTimeAsc(
                        month.atDay(1),
                        month.atEndOfMonth()
                ).stream()
                .limit(limit)
                .toList();
    }

    private Specification<Reservation> buildSpecification(AdminReservationSearchCondition condition) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (condition != null) {
                addDatePredicate(condition, root, criteriaBuilder, predicates);
            }
            if (condition != null && condition.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), condition.getStatus()));
            }
            if (condition != null && StringUtils.hasText(condition.getKeyword())) {
                String keyword = "%" + condition.getKeyword().toLowerCase() + "%";
                String kanaKeyword = "%" + normalizeKana(condition.getKeyword()) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerFamilyKana")), kanaKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerGivenKana")), kanaKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("customerEmail")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("reservationCode")), keyword)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addDatePredicate(
            AdminReservationSearchCondition condition,
            jakarta.persistence.criteria.Root<Reservation> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            List<Predicate> predicates
    ) {
        Integer year = condition.getYear();
        if (year == null) {
            return;
        }

        Integer month = condition.getMonth();
        Integer day = condition.getDay();
        if (month == null) {
            predicates.add(criteriaBuilder.between(
                    root.get("reservationDate"),
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31)
            ));
            return;
        }

        if (day == null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            predicates.add(criteriaBuilder.between(
                    root.get("reservationDate"),
                    yearMonth.atDay(1),
                    yearMonth.atEndOfMonth()
            ));
            return;
        }

        try {
            predicates.add(criteriaBuilder.equal(root.get("reservationDate"), LocalDate.of(year, month, day)));
        } catch (DateTimeException exception) {
            predicates.add(criteriaBuilder.disjunction());
        }
    }

    private String normalizeKana(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (codePoint >= '\u3041' && codePoint <= '\u3096') {
                builder.appendCodePoint(codePoint + 0x60);
            } else {
                builder.appendCodePoint(codePoint);
            }
        });
        return builder.toString().toLowerCase();
    }
    private Sort buildSort(AdminReservationSearchCondition condition) {
        if (condition == null || !StringUtils.hasText(condition.getSort())) {
            return Sort.by(
                    Sort.Order.desc("reservationDate"),
                    Sort.Order.asc("reservationTime"),
                    Sort.Order.desc("createdAt")
            );
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(condition.getDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (condition.getSort()) {
            case "reservationCode" -> Sort.by(new Sort.Order(direction, "reservationCode"))
                    .and(Sort.by(Sort.Order.desc("createdAt")));
            case "reservationDate" -> Sort.by(new Sort.Order(direction, "reservationDate"))
                    .and(Sort.by(Sort.Order.asc("reservationTime"), Sort.Order.desc("createdAt")));
            case "reservationTime" -> Sort.by(new Sort.Order(direction, "reservationTime"))
                    .and(Sort.by(Sort.Order.desc("reservationDate"), Sort.Order.desc("createdAt")));
            case "status" -> Sort.by(new Sort.Order(direction, "status"))
                    .and(Sort.by(Sort.Order.desc("reservationDate"), Sort.Order.asc("reservationTime"), Sort.Order.desc("createdAt")));
            default -> Sort.by(
                    Sort.Order.desc("reservationDate"),
                    Sort.Order.asc("reservationTime"),
                    Sort.Order.desc("createdAt")
            );
        };
    }

    private void validateCommon(ReservationRequest request) {
        if (!Boolean.TRUE.equals(request.getPrivacyAccepted())) {
            throw new BusinessException("繝励Λ繧､繝舌す繝ｼ繝昴Μ繧ｷ繝ｼ縺ｫ蜷梧э縺励※縺上□縺輔＞");
        }
        if (!StringUtils.hasText(request.getCustomerFamilyName())
                || !StringUtils.hasText(request.getCustomerGivenName())
                || !StringUtils.hasText(request.getCustomerFamilyKana())
                || !StringUtils.hasText(request.getCustomerGivenKana())
                || !StringUtils.hasText(request.getCustomerEmail())) {
            throw new BusinessException("蠢・磯・岼繧貞・蜉帙＠縺ｦ縺上□縺輔＞");
        }
    }

    private void validateReservation(ReservationRequest request) {
        if (request.getReservationDate() == null) {
            throw new BusinessException("莠育ｴ・律繧呈欠螳壹＠縺ｦ縺上□縺輔＞");
        }
        if (request.getReservationTime() == null) {
            throw new BusinessException("莠育ｴ・凾髢薙ｒ謖・ｮ壹＠縺ｦ縺上□縺輔＞");
        }
        if (request.getReservationCount() == null || request.getReservationCount() < 1 || request.getReservationCount() > 10) {
            throw new BusinessException("莠育ｴ・ｺｺ謨ｰ縺ｯ1縲・0蜷阪〒謖・ｮ壹＠縺ｦ縺上□縺輔＞");
        }
    }

    private Reservation createReservation(ReservationRequest request, ReservationStatus status) {
        validateReservation(request);
        WorkshopTimeSlot slot = timeSlotRepository.findByScheduleDateAndStartTimeForUpdate(
                        request.getReservationDate(),
                        request.getReservationTime()
                )
                .orElseThrow(() -> new BusinessException("Requested time slot was not found"));

        WorkshopSchedule schedule = slot.getSchedule();
        if (!schedule.isOpen()) {
            throw new BusinessException("Requested schedule is not open");
        }
        if (!slot.isActive()) {
            throw new BusinessException("Requested time slot is inactive");
        }
        int reservationCount = request.getReservationCount();
        if (slot.getReservedCount() + reservationCount > slot.getCapacity()) {
            throw new BusinessException("満席です");
        }

        int pricePerPerson = getPricePerPerson();
        int totalAmount = reservationCount * pricePerPerson;
        String reservationCode = generateReservationCode(request.getReservationDate());

        Reservation reservation = Reservation.builder()
                .reservationCode(reservationCode)
                .timeSlot(slot)
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .reservationCount(reservationCount)
                .customerFamilyName(request.getCustomerFamilyName())
                .customerGivenName(request.getCustomerGivenName())
                .customerFamilyKana(request.getCustomerFamilyKana())
                .customerGivenKana(request.getCustomerGivenKana())
                .customerEmail(request.getCustomerEmail())
                .customerTel(request.getCustomerTel())
                .customerMessage(request.getCustomerMessage())
                .totalAmount(totalAmount)
                .status(status)
                .build();
        reservationRepository.save(reservation);
        slot.setReservedCount(slot.getReservedCount() + reservationCount);
        return reservation;
    }

    private int getPricePerPerson() {
        SystemSetting setting = systemSettingRepository.findBySettingKey(PRICE_PER_PERSON_KEY)
                .orElseThrow(() -> new BusinessException("Price setting not found"));
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException exception) {
            throw new BusinessException("Price setting is invalid");
        }
    }

    private boolean hasAgeBreakdown(ReservationStatusUpdateRequest request) {
        return request.getMaleUnder10Count() != null
                || request.getMale20sCount() != null
                || request.getMale30sCount() != null
                || request.getMale40sCount() != null
                || request.getMale50sCount() != null
                || request.getMale60PlusCount() != null
                || request.getFemaleUnder10Count() != null
                || request.getFemale20sCount() != null
                || request.getFemale30sCount() != null
                || request.getFemale40sCount() != null
                || request.getFemale50sCount() != null
                || request.getFemale60PlusCount() != null;
    }

    private int sum(Integer... values) {
        int total = 0;
        for (Integer value : values) {
            if (value != null) {
                total += value;
            }
        }
        return total;
    }

    private int normalize(Integer value) {
        return value == null ? 0 : value;
    }

    private String generateReservationCode(LocalDate reservationDate) {
        long sequence = reservationRepository.countByReservationDate(reservationDate) + 1;
        String prefix = "WS-" + reservationDate.format(CODE_DATE_FORMAT) + "-";
        String code;
        do {
            code = prefix + String.format("%02d", sequence++);
        } while (reservationRepository.existsByReservationCode(code));
        return code;
    }
}

