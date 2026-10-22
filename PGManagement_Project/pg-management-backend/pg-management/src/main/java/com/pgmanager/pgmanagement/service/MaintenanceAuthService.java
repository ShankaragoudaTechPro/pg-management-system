
package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.MaintenanceLoginRequest;
import com.pgmanager.pgmanagement.dto.MaintenanceLoginResponse;
import com.pgmanager.pgmanagement.entity.MaintenanceStaff;
import com.pgmanager.pgmanagement.repository.MaintenanceStaffRepository;
import com.pgmanager.pgmanagement.security.JwtService;
import com.pgmanager.pgmanagement.security.Role;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaintenanceAuthService {

    private final MaintenanceStaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public MaintenanceLoginResponse login(
            MaintenanceLoginRequest request) {

        MaintenanceStaff staff = staffRepository
                .findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(), staff.getPassword())) {
            throw new BadCredentialsException(
                    "Invalid email or password");
        }

        if (!"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
            throw new DisabledException(
                    "Maintenance account is inactive");
        }

        String token = jwtService.generateToken(
                staff.getEmail(),
                Role.SUPER_MAINTENANCE,
                staff.getSessionVersion()
        );

        return MaintenanceLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .staffId(staff.getId())
                .name(staff.getName())
                .email(staff.getEmail())
                .role(Role.SUPER_MAINTENANCE.name())
                .build();
    }
}
