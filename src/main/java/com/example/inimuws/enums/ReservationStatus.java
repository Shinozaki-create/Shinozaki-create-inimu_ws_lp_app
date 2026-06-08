package com.example.inimuws.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING("受付待ち"),
    CONFIRMED("確定"),
    CANCELLED("キャンセル"),
    COMPLETED("完了"),
    NO_SHOW("無断キャンセル");

    private final String label;
}
