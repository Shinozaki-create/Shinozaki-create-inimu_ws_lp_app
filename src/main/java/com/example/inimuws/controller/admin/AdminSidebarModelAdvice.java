package com.example.inimuws.controller.admin;

import com.example.inimuws.service.InquiryService;
import com.example.inimuws.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.example.inimuws.controller.admin")
@RequiredArgsConstructor
public class AdminSidebarModelAdvice {

    private final ReservationService reservationService;
    private final InquiryService inquiryService;

    @ModelAttribute("sidebarReservationPendingCount")
    public long sidebarReservationPendingCount() {
        return reservationService.countPending();
    }

    @ModelAttribute("sidebarInquiryOpenCount")
    public long sidebarInquiryOpenCount() {
        return inquiryService.countOpen();
    }

    @ModelAttribute("adminCssVersion")
    public long adminCssVersion() {
        return System.currentTimeMillis();
    }
}
