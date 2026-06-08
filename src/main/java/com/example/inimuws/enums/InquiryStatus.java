package com.example.inimuws.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    OPEN("未対応"),
    IN_PROGRESS("対応中"),
    CLOSED("完了");

    private final String label;
}
