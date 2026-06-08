package com.example.inimuws.dto;

import com.example.inimuws.enums.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryStatusUpdateRequest {

    @NotNull
    private InquiryStatus status;

    private String adminMemo;
}
