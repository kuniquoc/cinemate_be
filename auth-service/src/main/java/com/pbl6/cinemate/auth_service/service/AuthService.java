package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.payload.request.*;
import com.pbl6.cinemate.auth_service.payload.response.LoginResponse;
import com.pbl6.cinemate.auth_service.payload.response.SignUpResponse;
import jakarta.transaction.Transactional;

public interface AuthService {
    SignUpResponse signUp(SignUpRequest signUpRequest);

    @Transactional
    void verifyAccount(AccountVerificationRequest accountVerificationRequest);

    LoginResponse login(LoginRequest loginRequest);

    void forgotPassword(ForgotPasswordRequest request);

    boolean verifyOTP(String email, String contentToken);

    @Transactional
    void resetPassword(ResetPasswordRequest request);
}
