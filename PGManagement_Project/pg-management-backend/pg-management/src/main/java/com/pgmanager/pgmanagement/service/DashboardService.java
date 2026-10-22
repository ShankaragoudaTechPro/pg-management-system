package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.DashboardResponse;

import java.util.List;

public interface DashboardService {

    List<DashboardResponse> getMyDashboard(
            String ownerEmail
    );

    DashboardResponse getPgDashboard(
            String ownerEmail,
            Long pgId
    );
}