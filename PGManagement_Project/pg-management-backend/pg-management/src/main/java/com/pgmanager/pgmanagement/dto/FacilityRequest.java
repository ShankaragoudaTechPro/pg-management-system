package com.pgmanager.pgmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacilityRequest {

    @NotBlank(message = "Facility name is required")
    @Size(max = 100, message = "Facility name cannot exceed 100 characters")
    private String name;
}