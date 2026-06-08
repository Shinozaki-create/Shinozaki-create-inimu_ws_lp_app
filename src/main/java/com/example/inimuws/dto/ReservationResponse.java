package com.example.inimuws.dto;

public record ReservationResponse(
        String type,
        String reservationCode,
        String message
) {
    public static ReservationResponse reservation(String reservationCode) {
        return new ReservationResponse("reservation", reservationCode, "予約を受け付けました");
    }

    public static ReservationResponse inquiry() {
        return new ReservationResponse("inquiry", null, "お問い合わせを受け付けました");
    }
}
