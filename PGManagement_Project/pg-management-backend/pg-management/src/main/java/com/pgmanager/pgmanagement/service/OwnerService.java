package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.*;

public interface OwnerService {

    OwnerResponse registerOwner(
            OwnerRegistrationRequest request
    );

    LoginResponse loginOwner(
            OwnerLoginRequest request
    );

    String forgotPassword(
            ForgotPasswordRequest request
    );

    String verifyOtp(
            VerifyOtpRequest request
    );

    String resetPassword(
            ResetPasswordRequest request
    );

    OwnerProfileResponse getMyProfile(
            String email
    );

    OwnerProfileResponse updateMyProfile(
            String email,
            OwnerUpdateRequest request
    );

    String changePassword(
            String email,
            ChangePasswordRequest request
    );
}