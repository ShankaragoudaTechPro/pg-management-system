
package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.*;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.OwnerStatus;
import com.pgmanager.pgmanagement.entity.PasswordResetToken;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.PasswordResetTokenRepository;
import com.pgmanager.pgmanagement.security.JwtService;
import com.pgmanager.pgmanagement.security.Role;
import com.pgmanager.pgmanagement.service.OwnerService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    // =========================================================
    // REGISTER OWNER
    // =========================================================

    @Override
    @Transactional
    public OwnerResponse registerOwner(
            OwnerRegistrationRequest request) {

        String name = request.getName().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);
        String mobile = request.getMobile().trim();

        if (ownerRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "Email already registered");
        }

        if (ownerRepository.existsByMobile(mobile)) {
            throw new RuntimeException(
                    "Mobile number already registered");
        }

        Owner owner = Owner.builder()
                .name(name)
                .mobile(mobile)
                .email(email)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()))
                .role(Role.OWNER)
                .status(OwnerStatus.ACTIVE)
                .build();

        Owner savedOwner = ownerRepository.save(owner);

        return OwnerResponse.builder()
                .id(savedOwner.getId())
                .name(savedOwner.getName())
                .mobile(savedOwner.getMobile())
                .email(savedOwner.getEmail())
                .status(savedOwner.getStatus())
                .createdAt(savedOwner.getCreatedAt())
                .build();
    }

    // =========================================================
    // LOGIN OWNER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public LoginResponse loginOwner(
            OwnerLoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"));

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        owner.getPassword());

        if (!passwordMatches) {
            throw new RuntimeException(
                    "Invalid email or password");
        }

        // Only active accounts can log in.
        if (owner.getStatus() != OwnerStatus.ACTIVE) {
            throw new RuntimeException(
                    "Account is not active. Please contact support.");
        }

        if (owner.getRole() == null) {
            throw new RuntimeException(
                    "Account role is not configured.");
        }

        // Generate JWT using the current database role
        // and session version.
        String token = jwtService.generateToken(
                owner.getEmail(),
                owner.getRole(),
                owner.getSessionVersion());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .ownerId(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .build();
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @Override
    @Transactional
    public String forgotPassword(
            ForgotPasswordRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No account found with this email"));

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000));

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .owner(owner)
                        .otp(otp)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(10))
                        .used(false)
                        .build();

        passwordResetTokenRepository.save(resetToken);

        /*
         * Development only.
         * For production, integrate an SMS OTP provider.
         */
        System.out.println(
                "PASSWORD RESET OTP GENERATED FOR REGISTERED MOBILE: "
                        + owner.getMobile());

        // Do not print the actual OTP in production.
        return "OTP generated successfully";
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    @Override
    public String verifyOtp(
            VerifyOtpRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No account found with this email"));

        PasswordResetToken resetToken =
                getLatestValidToken(owner);

        if (!resetToken.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        return "OTP verified successfully";
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Override
    @Transactional
    public String resetPassword(
            ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new RuntimeException(
                    "Passwords do not match");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "No account found with this email"));

        PasswordResetToken resetToken =
                getLatestValidToken(owner);

        if (!resetToken.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        owner.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()));

        ownerRepository.save(owner);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Password reset successfully";
    }

    // =========================================================
    // GET MY PROFILE
    // =========================================================

    @Override
    public OwnerProfileResponse getMyProfile(String email) {

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Owner not found"));

        return mapProfile(owner);
    }

    // =========================================================
    // UPDATE MY PROFILE
    // =========================================================

    @Override
    @Transactional
    public OwnerProfileResponse updateMyProfile(
            String email,
            OwnerUpdateRequest request) {

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Owner not found"));

        // Email is the login ID and JWT subject.
        // Do not allow it to change.
        if (!owner.getEmail()
                .equalsIgnoreCase(request.getEmail())) {

            throw new RuntimeException(
                    "Login email cannot be changed");
        }

        if (!owner.getMobile().equals(request.getMobile())
                && ownerRepository.existsByMobile(
                request.getMobile())) {

            throw new RuntimeException(
                    "Mobile number already registered");
        }

        owner.setName(request.getName());
        owner.setMobile(request.getMobile());

        Owner updatedOwner = ownerRepository.save(owner);

        return mapProfile(updatedOwner);
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Override
    @Transactional
    public String changePassword(
            String email,
            ChangePasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new RuntimeException(
                    "New password and confirm password do not match");
        }

        Owner owner = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Owner not found"));

        boolean currentPasswordMatches =
                passwordEncoder.matches(
                        request.getCurrentPassword(),
                        owner.getPassword());

        if (!currentPasswordMatches) {
            throw new RuntimeException(
                    "Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                owner.getPassword())) {

            throw new RuntimeException(
                    "New password must be different from current password");
        }

        owner.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()));

        ownerRepository.save(owner);

        return "Password changed successfully";
    }

    // =========================================================
    // GET LATEST VALID OTP
    // =========================================================

    private PasswordResetToken getLatestValidToken(
            Owner owner) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findTopByOwnerOrderByCreatedAtDesc(owner)
                        .orElseThrow(() ->
                                new RuntimeException("OTP not found"));

        if (resetToken.isUsed()) {
            throw new RuntimeException(
                    "OTP has already been used");
        }

        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException("OTP has expired");
        }

        return resetToken;
    }

    // =========================================================
    // PROFILE MAPPER
    // =========================================================

    private OwnerProfileResponse mapProfile(Owner owner) {

        return OwnerProfileResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .mobile(owner.getMobile())
                .email(owner.getEmail())
                .status(owner.getStatus())
                .createdAt(owner.getCreatedAt())
                .updatedAt(owner.getUpdatedAt())
                .build();
    }
}
