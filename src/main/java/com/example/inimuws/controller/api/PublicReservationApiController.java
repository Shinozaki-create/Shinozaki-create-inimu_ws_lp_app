package com.example.inimuws.controller.api;

import com.example.inimuws.dto.ReservationRequest;
import com.example.inimuws.dto.ReservationResponse;
import com.example.inimuws.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class PublicReservationApiController {

    private final ReservationService reservationService;

    @PostMapping
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return reservationService.createReservationOrInquiry(request);
    }
}
