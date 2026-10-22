
package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.MaintenanceStaffRegistrationRequest;
import com.pgmanager.pgmanagement.entity.MaintenanceStaff;
import com.pgmanager.pgmanagement.service.MaintenanceStaffRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/maintenance/auth")
@RequiredArgsConstructor
public class MaintenanceStaffRegistrationController {

    private final MaintenanceStaffRegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<MaintenanceStaffRegistrationResponse> register(
            @Valid @RequestBody MaintenanceStaffRegistrationRequest request) {

        MaintenanceStaff staff = registrationService.register(request);

        MaintenanceStaffRegistrationResponse response =
                new MaintenanceStaffRegistrationResponse(
                        staff.getId(),
                        staff.getName(),
                        staff.getEmail(),
                        staff.getMobile(),
                        staff.getStatus()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public record MaintenanceStaffRegistrationResponse(
            Long id,
            String name,
            String email,
            String mobile,
            String status
    ) {}
}
