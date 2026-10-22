package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.DashboardResponse;
import com.pgmanager.pgmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // =========================================================
    // ALL PG DASHBOARDS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<DashboardResponse>> getDashboard(
            Authentication authentication) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dashboardService.getMyDashboard(ownerEmail));
    }

    // =========================================================
    // SINGLE PG DASHBOARD
    // =========================================================

    @GetMapping("/pg/{pgId}")
    public ResponseEntity<DashboardResponse> getPgDashboard(
            Authentication authentication,
            @PathVariable Long pgId) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(dashboardService.getPgDashboard(ownerEmail, pgId));
    }
}