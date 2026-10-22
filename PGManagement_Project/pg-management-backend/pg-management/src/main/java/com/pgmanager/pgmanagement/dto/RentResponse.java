package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.PaymentMode;
import com.pgmanager.pgmanagement.entity.RentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class RentResponse {

    private Long id;

    private Long tenantId;

    private String tenantName;

    private Long pgId;

    private String pgName;

    private Double rentAmount;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private LocalDate dueDate;

    private LocalDate paidDate;

    private RentStatus status;

    private PaymentMode paymentMode;

    private String paymentNote;
}