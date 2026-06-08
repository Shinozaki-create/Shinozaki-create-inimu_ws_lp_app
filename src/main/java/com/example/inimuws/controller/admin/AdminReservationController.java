package com.example.inimuws.controller.admin;

import com.example.inimuws.dto.AdminReservationSearchCondition;
import com.example.inimuws.dto.ReservationRequest;
import com.example.inimuws.dto.ReservationStatusUpdateRequest;
import com.example.inimuws.entity.Reservation;
import com.example.inimuws.enums.ReservationStatus;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.service.ReservationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public String list(@ModelAttribute("condition") AdminReservationSearchCondition condition, Model model) {
        model.addAttribute("reservations", reservationService.searchReservations(condition));
        model.addAttribute("reservationStatuses", ReservationStatus.values());
        return "admin/reservations";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("reservationRequest", createReservationRequest());
        model.addAttribute("reservationCountOptions", reservationCountOptions());
        return "admin/reservation-create";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("reservationRequest") ReservationRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        model.addAttribute("reservationCountOptions", reservationCountOptions());
        if (bindingResult.hasErrors()) {
            return "admin/reservation-create";
        }

        try {
            Reservation reservation = reservationService.createAdminReservation(request);
            return "redirect:/admin/reservations/" + reservation.getId();
        } catch (BusinessException exception) {
            model.addAttribute("reservationError", exception.getMessage());
            return "admin/reservation-create";
        }
    }

    @GetMapping(value = "/code", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String previewCode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reservationService.previewReservationCode(date);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Reservation reservation = reservationService.findById(id);
        ReservationStatusUpdateRequest updateRequest = new ReservationStatusUpdateRequest();
        updateRequest.setStatus(reservation.getStatus());
        updateRequest.setParticipantCount(reservation.effectiveParticipantCount());
        updateRequest.setMaleCount(reservation.effectiveMaleCount());
        updateRequest.setFemaleCount(reservation.effectiveFemaleCount());
        if (reservation.hasAgeBreakdown()) {
            updateRequest.setMaleUnder10Count(reservation.getMaleUnder10Count());
            updateRequest.setMale20sCount(reservation.getMale20sCount());
            updateRequest.setMale30sCount(reservation.getMale30sCount());
            updateRequest.setMale40sCount(reservation.getMale40sCount());
            updateRequest.setMale50sCount(reservation.getMale50sCount());
            updateRequest.setMale60PlusCount(reservation.getMale60PlusCount());
            updateRequest.setFemaleUnder10Count(reservation.getFemaleUnder10Count());
            updateRequest.setFemale20sCount(reservation.getFemale20sCount());
            updateRequest.setFemale30sCount(reservation.getFemale30sCount());
            updateRequest.setFemale40sCount(reservation.getFemale40sCount());
            updateRequest.setFemale50sCount(reservation.getFemale50sCount());
            updateRequest.setFemale60PlusCount(reservation.getFemale60PlusCount());
        }
        updateRequest.setAdminMemo(reservation.getAdminMemo());
        model.addAttribute("reservation", reservation);
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("reservationStatuses", ReservationStatus.values());
        model.addAttribute("ageCountOptions", IntStream.rangeClosed(1, 10).boxed().toList());
        return "admin/reservation-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @ModelAttribute ReservationStatusUpdateRequest request) {
        reservationService.updateStatus(id, request);
        return "redirect:/admin/reservations/" + id;
    }

    @GetMapping("/csv")
    public void csv(@ModelAttribute AdminReservationSearchCondition condition, HttpServletResponse response) throws IOException {
        List<Reservation> reservations = reservationService.searchReservations(condition);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=reservations.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.println("予約番号,日付,時間,人数,氏名,メール,金額,ステータス");
            for (Reservation reservation : reservations) {
                writer.printf("%s,%s,%s,%d,%s,%s,%d,%s%n",
                        csv(reservation.getReservationCode()),
                        reservation.getReservationDate(),
                        reservation.getReservationTime(),
                        reservation.getReservationCount(),
                        csv(reservation.customerName()),
                        csv(reservation.getCustomerEmail()),
                        reservation.getTotalAmount(),
                        reservation.getStatus().name()
                );
            }
        }
    }

    private ReservationRequest createReservationRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setInquiryOnly(false);
        request.setReservationCount(1);
        request.setPrivacyAccepted(true);
        return request;
    }

    private List<Integer> reservationCountOptions() {
        return IntStream.rangeClosed(1, 10).boxed().toList();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
