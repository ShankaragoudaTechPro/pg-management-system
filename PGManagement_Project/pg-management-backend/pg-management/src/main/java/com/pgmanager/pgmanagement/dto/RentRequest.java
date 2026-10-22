package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RentRequest {

    @NotNull(message = "Rent amount is required")
    @Positive(message = "Rent amount must be greater than zero")
    private Double rentAmount;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private PaymentMode paymentMode;

    private String paymentNote;
}