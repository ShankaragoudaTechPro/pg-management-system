package com.pgmanager.pgmanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardResponse {

    private Long pgId;

    private String pgName;

    private long totalRooms;

    private long totalBeds;

    private long occupiedBeds;

    private long availableBeds;

    private long activeTenants;

    private double occupancyPercentage;

    private double rentDue;

    private double rentOverdue;

    private double rentCollected;
}