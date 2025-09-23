package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.payload.general.ResponseData;
import com.pbl6.cinemate.auth_service.payload.request.*;
import com.pbl6.cinemate.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping(ApiPath.SIGN_UP)
    public ResponseEntity<ResponseData> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        ResponseData responseData = ResponseData.success(authService.signUp(signUpRequest),
                FeedbackMessage.SIGNED_UP);
        return ResponseEntity.ok(responseData);
    }

    @PatchMapping(ApiPath.VERIFY_ACCOUNT)
    public ResponseEntity<ResponseData> verifyAccount(@Valid @RequestBody AccountVerificationRequest
                                                              accountVerificationRequest) {
        authService.verifyAccount(accountVerificationRequest);
        ResponseData responseData = ResponseData
                .successWithoutMetaAndData(FeedbackMessage.ACCOUNT_VERIFIED);
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.LOGIN)
    public ResponseEntity<ResponseData> login(@Valid @RequestBody LoginRequest loginRequest) {
        ResponseData responseData = ResponseData.success(authService.login(loginRequest),
                FeedbackMessage.LOGGED_IN);
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.FORGOT_PASSWORD)
    public ResponseEntity<ResponseData> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.FORGOT_PASSWORD);
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.VERIFY_OTP)
    public ResponseEntity<ResponseData> verifyOTP(@Valid @RequestBody OtpVerificationRequest request) {
        ResponseData responseData = ResponseData.success(authService.verifyOTP(request.getEmail(), request.getOtp()),
                FeedbackMessage.OTP_VERIFIED);
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.RESET_PASSWORD)
    public ResponseEntity<ResponseData> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.RESET_PASSWORD);
        return ResponseEntity.ok(responseData);
    }
}
