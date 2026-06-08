package com.example.inimuws.dto;

import com.example.inimuws.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReservationSearchCondition {

    private Integer year;

    private Integer month;

    private Integer day;

    private ReservationStatus status;

    private String keyword;

    private String sort;

    private String direction;
}
