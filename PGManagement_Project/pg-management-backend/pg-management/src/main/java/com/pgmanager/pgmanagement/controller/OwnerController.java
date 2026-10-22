package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.ChangePasswordRequest;
import com.pgmanager.pgmanagement.dto.OwnerProfileResponse;
import com.pgmanager.pgmanagement.dto.OwnerUpdateRequest;
import com.pgmanager.pgmanagement.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    // =========================================================
    // GET MY PROFILE
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<OwnerProfileResponse> getMyProfile(
            Authentication authentication) {

        String email = authentication.getName();

        OwnerProfileResponse response =
                ownerService.getMyProfile(email);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UPDATE MY PROFILE
    // =========================================================

    @PutMapping("/me")
    public ResponseEntity<OwnerProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody OwnerUpdateRequest request) {

        String email = authentication.getName();

        OwnerProfileResponse response =
                ownerService.updateMyProfile(
                        email,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @PostMapping("/me/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        String email = authentication.getName();

        String response =
                ownerService.changePassword(
                        email,
                        request
                );

        return ResponseEntity.ok(response);
    }
}