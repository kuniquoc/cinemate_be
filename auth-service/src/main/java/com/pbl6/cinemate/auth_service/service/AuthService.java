package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.payload.request.*;
import com.pbl6.cinemate.auth_service.payload.response.LoginResponse;
import com.pbl6.cinemate.auth_service.payload.response.SignUpResponse;
import com.pbl6.cinemate.auth_service.payload.response.VerifyTokenResponse;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface AuthService {
    void verifyEmail(VerifyEmailRequest request);

    SignUpResponse signUp(SignUpRequest signUpRequest);

    @Transactional
    VerifyTokenResponse verifyToken(VerifyTokenRequest verifyTokenRequest);

    LoginResponse login(LoginRequest loginRequest);

    void forgotPassword(ForgotPasswordRequest request);

    boolean verifyOTP(String email, String contentToken);

    @Transactional
    void resetPassword(ResetPasswordRequest request);

    void logout(LogoutRequest logoutRequest);

    void changePassword(UUID userId, PasswordChangingRequest passwordChangingRequest);
}
