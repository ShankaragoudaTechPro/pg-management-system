package com.pgmanager.pgmanagement.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TenantResponse {

    private Long id;

    private String name;

    // Compatibility fields expected by the Angular tenant model.
    private String fullName;

    private String mobile;

    private String mobileNumber;

    private String email;

    private String gender;

    private LocalDate joiningDate;

    private String emergencyContact;

    private String emergencyContactName;

    private Long pgId;

    private String pgName;

    private Long roomId;

    private String roomNumber;

    private Long bedId;

    private Integer bedNumber;

    private Double monthlyRent;

    private Double securityDeposit;

    private boolean active;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}