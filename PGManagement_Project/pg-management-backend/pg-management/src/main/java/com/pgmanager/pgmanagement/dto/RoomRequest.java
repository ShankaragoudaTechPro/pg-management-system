package com.pgmanager.pgmanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @Min(value = 1, message = "Sharing must be at least 1")
    @Max(value = 4, message = "Sharing cannot be more than 4")
    private Integer sharing;

    private Integer floor;
}