package com.example.inimuws.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeSlotUpdateRequest {

    @Min(0)
    private int capacity;

    private boolean active;
}
