package com.pgmanager.pgmanagement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(max = 100, message = "Tenant name cannot exceed 100 characters")
    @JsonAlias("fullName")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Enter a valid 10-digit Indian mobile number"
    )
    @JsonAlias("mobileNumber")
    private String mobile;

    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private String emergencyContact;

    private String emergencyContactName;

    // Optional sensitive field; never returned by API responses.
    private String aadhaarNumber;

    @NotNull(message = "PG ID is required")
    private Long pgId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotNull(message = "Bed ID is required")
    private Long bedId;

    @NotNull(message = "Monthly rent is required")
    @Positive(message = "Monthly rent must be greater than zero")
    private Double monthlyRent;

    @NotNull(message = "Security deposit is required")
    @PositiveOrZero(message = "Security deposit cannot be negative")
    private Double securityDeposit;
}