package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MarkRentPaidRequest {

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private LocalDate paidDate;

    @Size(max = 500, message = "Payment note cannot exceed 500 characters")
    private String paymentNote;
}
