package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.*;
import com.pgmanager.pgmanagement.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OwnerService ownerService;

    @PostMapping("/register")
    public ResponseEntity<OwnerResponse> registerOwner(
            @Valid @RequestBody OwnerRegistrationRequest request) {

        OwnerResponse response =
                ownerService.registerOwner(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody OwnerLoginRequest request) {

        LoginResponse response =
                ownerService.loginOwner(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String response =
                ownerService.forgotPassword(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        String response =
                ownerService.verifyOtp(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        String response =
                ownerService.resetPassword(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {

        return ResponseEntity.ok(
                "Logout successful"
        );
    }

//    POST /api/v1/auth/register
//    POST /api/v1/auth/login
//    POST /api/v1/auth/forgot-password
//    POST /api/v1/auth/verify-otp
//    POST /api/v1/auth/reset-password

}