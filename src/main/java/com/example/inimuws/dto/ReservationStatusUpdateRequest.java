package com.example.inimuws.dto;

import com.example.inimuws.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationStatusUpdateRequest {

    @NotNull
    private ReservationStatus status;

    private Integer participantCount;
    private Integer maleCount;
    private Integer femaleCount;

    private Integer maleUnder10Count;
    private Integer male20sCount;
    private Integer male30sCount;
    private Integer male40sCount;
    private Integer male50sCount;
    private Integer male60PlusCount;

    private Integer femaleUnder10Count;
    private Integer female20sCount;
    private Integer female30sCount;
    private Integer female40sCount;
    private Integer female50sCount;
    private Integer female60PlusCount;

    private String adminMemo;
}
