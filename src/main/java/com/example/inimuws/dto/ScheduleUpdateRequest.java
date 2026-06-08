package com.example.inimuws.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleUpdateRequest {

    @NotNull
    private Boolean open;

    private String note;
}
