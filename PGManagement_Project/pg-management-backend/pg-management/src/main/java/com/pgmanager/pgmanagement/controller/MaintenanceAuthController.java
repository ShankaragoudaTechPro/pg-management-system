
package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.MaintenanceLoginRequest;
import com.pgmanager.pgmanagement.dto.MaintenanceLoginResponse;
import com.pgmanager.pgmanagement.service.MaintenanceAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance/auth")
@RequiredArgsConstructor
public class MaintenanceAuthController {

    private final MaintenanceAuthService maintenanceAuthService;

    @PostMapping("/login")
    public ResponseEntity<MaintenanceLoginResponse> login(
            @Valid @RequestBody MaintenanceLoginRequest request) {

        return ResponseEntity.ok(
                maintenanceAuthService.login(request));
    }
}
