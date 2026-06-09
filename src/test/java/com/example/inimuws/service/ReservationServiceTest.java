package com.example.inimuws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.inimuws.dto.AdminReservationSearchCondition;
import com.example.inimuws.dto.ReservationRequest;
import com.example.inimuws.dto.ReservationResponse;
import com.example.inimuws.dto.ReservationStatusUpdateRequest;
import com.example.inimuws.entity.Reservation;
import com.example.inimuws.entity.WorkshopTimeSlot;
import com.example.inimuws.enums.ReservationStatus;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.repository.InquiryRepository;
import com.example.inimuws.repository.ReservationRepository;
import com.example.inimuws.repository.WorkshopTimeSlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WorkshopTimeSlotRepository timeSlotRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Test
    void createsReservationAndIncreasesReservedCount() {
        ReservationResponse response = reservationService.createReservationOrInquiry(reservationRequest(LocalTime.of(11, 0), 2));

        assertThat(response.type()).isEqualTo("reservation");
        assertThat(response.reservationCode()).startsWith("WS-260606-");

        WorkshopTimeSlot slot = timeSlotRepository.findBySchedule_ScheduleDateOrderByStartTimeAsc(LocalDate.of(2026, 6, 6)).get(0);
        assertThat(slot.getReservedCount()).isEqualTo(2);
    }

    @Test
    void createsAdminReservationWithConfirmedStatusAndIncreasesReservedCount() {
        WorkshopTimeSlot slot = timeSlotRepository.findBySchedule_ScheduleDateOrderByStartTimeAsc(LocalDate.of(2026, 6, 6))
                .stream()
                .filter(candidate -> candidate.getStartTime().equals(LocalTime.of(11, 0)))
                .findFirst()
                .orElseThrow();
        int beforeReservedCount = slot.getReservedCount();

        Reservation reservation = reservationService.createAdminReservation(reservationRequest(LocalTime.of(11, 0), 2));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getReservationCode()).startsWith("WS-260606-");

        WorkshopTimeSlot updatedSlot = timeSlotRepository.findBySchedule_ScheduleDateOrderByStartTimeAsc(LocalDate.of(2026, 6, 6))
                .stream()
                .filter(candidate -> candidate.getStartTime().equals(LocalTime.of(11, 0)))
                .findFirst()
                .orElseThrow();
        assertThat(updatedSlot.getReservedCount()).isEqualTo(beforeReservedCount + 2);
    }

    @Test
    void previewsReservationCodeFromReservationDate() {
        assertThat(reservationService.previewReservationCode(LocalDate.of(2026, 6, 13)))
                .isEqualTo("WS-260613-03");
    }

    @Test
    void createsInquiryWhenInquiryOnlyIsTrue() {
        long beforeCount = inquiryRepository.count();
        ReservationRequest request = commonRequest();
        request.setInquiryOnly(true);
        request.setCustomerMessage("日程について教えてください。");

        ReservationResponse response = reservationService.createReservationOrInquiry(request);

        assertThat(response.type()).isEqualTo("inquiry");
        assertThat(inquiryRepository.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    void rejectsReservationWhenSlotIsFull() {
        assertThatThrownBy(() -> reservationService.createReservationOrInquiry(reservationRequest(LocalTime.of(15, 0), 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancellingReservationDecreasesReservedCountOnce() {
        ReservationResponse response = reservationService.createReservationOrInquiry(reservationRequest(LocalTime.of(11, 0), 3));
        Reservation reservation = reservationRepository.findByReservationCode(response.reservationCode()).orElseThrow();

        ReservationStatusUpdateRequest updateRequest = new ReservationStatusUpdateRequest();
        updateRequest.setStatus(ReservationStatus.CANCELLED);
        reservationService.updateStatus(reservation.getId(), updateRequest);
        reservationService.updateStatus(reservation.getId(), updateRequest);

        WorkshopTimeSlot slot = timeSlotRepository.findBySchedule_ScheduleDateOrderByStartTimeAsc(LocalDate.of(2026, 6, 6))
                .stream()
                .filter(candidate -> candidate.getStartTime().equals(LocalTime.of(11, 0)))
                .findFirst()
                .orElseThrow();
        assertThat(slot.getReservedCount()).isZero();
    }

    @Test
    void updatingReservationStoresParticipantBreakdownAndRecalculatesAmount() {
        ReservationResponse response = reservationService.createReservationOrInquiry(reservationRequest(LocalTime.of(11, 0), 4));
        Reservation reservation = reservationRepository.findByReservationCode(response.reservationCode()).orElseThrow();

        ReservationStatusUpdateRequest updateRequest = new ReservationStatusUpdateRequest();
        updateRequest.setStatus(ReservationStatus.CONFIRMED);
        updateRequest.setParticipantCount(3);
        updateRequest.setMaleCount(1);
        updateRequest.setFemaleCount(2);
        updateRequest.setAdminMemo("調整済み");

        Reservation updated = reservationService.updateStatus(reservation.getId(), updateRequest);

        assertThat(updated.getParticipantCount()).isEqualTo(3);
        assertThat(updated.getMaleCount()).isEqualTo(1);
        assertThat(updated.getFemaleCount()).isEqualTo(2);
        assertThat(updated.getTotalAmount()).isEqualTo(16500);
    }

    @Test
    void updatingReservationStoresAgeBreakdownAndRecalculatesAmount() {
        ReservationResponse response = reservationService.createReservationOrInquiry(reservationRequest(LocalTime.of(11, 0), 4));
        Reservation reservation = reservationRepository.findByReservationCode(response.reservationCode()).orElseThrow();

        ReservationStatusUpdateRequest updateRequest = new ReservationStatusUpdateRequest();
        updateRequest.setStatus(ReservationStatus.CONFIRMED);
        updateRequest.setMaleUnder10Count(1);
        updateRequest.setMale20sCount(2);
        updateRequest.setFemale30sCount(1);
        updateRequest.setFemale60PlusCount(1);
        updateRequest.setAdminMemo("年齢構成の調整済み");

        Reservation updated = reservationService.updateStatus(reservation.getId(), updateRequest);

        assertThat(updated.getParticipantCount()).isEqualTo(5);
        assertThat(updated.getMaleCount()).isEqualTo(3);
        assertThat(updated.getFemaleCount()).isEqualTo(2);
        assertThat(updated.getMaleUnder10Count()).isEqualTo(1);
        assertThat(updated.getMale20sCount()).isEqualTo(2);
        assertThat(updated.getFemale30sCount()).isEqualTo(1);
        assertThat(updated.getFemale60PlusCount()).isEqualTo(1);
        assertThat(updated.getTotalAmount()).isEqualTo(27500);
    }

    @Test
    void searchReservationsCanFilterByYearAndMonth() {
        AdminReservationSearchCondition condition = new AdminReservationSearchCondition();
        condition.setYear(2026);
        condition.setMonth(6);

        assertThat(reservationService.searchReservations(condition))
                .isNotEmpty()
                .allMatch(reservation -> reservation.getReservationDate().getYear() == 2026
                        && reservation.getReservationDate().getMonthValue() == 6);
    }

    @Test
    void searchReservationsCanFilterByFullDate() {
        AdminReservationSearchCondition condition = new AdminReservationSearchCondition();
        condition.setYear(2026);
        condition.setMonth(6);
        condition.setDay(13);

        assertThat(reservationService.searchReservations(condition))
                .isNotEmpty()
                .allMatch(reservation -> reservation.getReservationDate().equals(LocalDate.of(2026, 6, 13)));
    }

    @Test
    void searchReservationsMatchesFuriganaKeyword() {
        AdminReservationSearchCondition condition = new AdminReservationSearchCondition();
        condition.setKeyword("こばやし");

        assertThat(reservationService.searchReservations(condition))
                .anyMatch(reservation -> reservation.customerFuriganaHiragana().contains("こばやし"));
    }

    private ReservationRequest reservationRequest(LocalTime time, int count) {
        ReservationRequest request = commonRequest();
        request.setInquiryOnly(false);
        request.setReservationDate(LocalDate.of(2026, 6, 6));
        request.setReservationTime(time);
        request.setReservationCount(count);
        request.setCustomerMessage("2名で予約します。");
        return request;
    }

    private ReservationRequest commonRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setCustomerFamilyName("山田");
        request.setCustomerGivenName("花子");
        request.setCustomerFamilyKana("ヤマダ");
        request.setCustomerGivenKana("ハナコ");
        request.setCustomerEmail("hanako@example.com");
        request.setCustomerTel("09012345678");
        request.setPrivacyAccepted(true);
        return request;
    }
}
