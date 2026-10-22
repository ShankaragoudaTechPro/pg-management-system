
package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.MaintenanceStaffRegistrationRequest;
import com.pgmanager.pgmanagement.entity.MaintenanceStaff;
import com.pgmanager.pgmanagement.repository.MaintenanceStaffRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MaintenanceStaffRegistrationService {

    private final MaintenanceStaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MaintenanceStaff register(
            MaintenanceStaffRegistrationRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String mobile = request.getMobile().trim();

        if (staffRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        if (staffRepository.existsByMobile(mobile)) {
            throw new IllegalArgumentException(
                    "Mobile number is already registered");
        }

        MaintenanceStaff staff = MaintenanceStaff.builder()
                .name(request.getName().trim())
                .email(email)
                .mobile(mobile)
                .password(passwordEncoder.encode(
                        request.getPassword()))
                .status("ACTIVE")
                .sessionVersion(0L)
                .build();

        return staffRepository.save(staff);
    }
}
