package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.entity.UserPrincipal;
import com.pbl6.cinemate.auth_service.payload.general.ResponseData;
import com.pbl6.cinemate.auth_service.payload.request.*;
import com.pbl6.cinemate.auth_service.security.annotation.CurrentUser;
import com.pbl6.cinemate.auth_service.service.AuthService;
import com.pbl6.cinemate.auth_service.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping(ApiPath.SIGN_UP)
    public ResponseEntity<ResponseData> signUp(@Valid @RequestBody SignUpRequest signUpRequest,
                                               HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(authService.signUp(signUpRequest),
                FeedbackMessage.SIGNED_UP, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PatchMapping(ApiPath.VERIFY_ACCOUNT)
    public ResponseEntity<ResponseData> verifyAccount(@Valid @RequestBody AccountVerificationRequest
                                                              accountVerificationRequest, HttpServletRequest request) {
        authService.verifyAccount(accountVerificationRequest);
        ResponseData responseData = ResponseData
                .successWithoutMetaAndData(FeedbackMessage.ACCOUNT_VERIFIED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.LOGIN)
    public ResponseEntity<ResponseData> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest httpServletRequest) {
        ResponseData responseData = ResponseData.success(authService.login(loginRequest),
                FeedbackMessage.LOGGED_IN, httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.FORGOT_PASSWORD)
    public ResponseEntity<ResponseData> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
                                                             HttpServletRequest request) {
        authService.forgotPassword(forgotPasswordRequest);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.FORGOT_PASSWORD,
                request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.VERIFY_OTP)
    public ResponseEntity<ResponseData> verifyOTP(@Valid @RequestBody OtpVerificationRequest request,
                                                  HttpServletRequest httpServletRequest) {
        ResponseData responseData = ResponseData.success(authService.verifyOTP(request.getEmail(), request.getOtp()),
                FeedbackMessage.OTP_VERIFIED, httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.RESET_PASSWORD)
    public ResponseEntity<ResponseData> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                      HttpServletRequest httpServletRequest) {
        authService.resetPassword(request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.RESET_PASSWORD,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.LOGOUT)
    public ResponseEntity<ResponseData> logout(@Valid @RequestBody LogoutRequest logoutRequest, HttpServletRequest request) {
        authService.logout(logoutRequest);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.LOGGED_OUT,
                request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PatchMapping(ApiPath.CHANGE_PASSWORD)
    public ResponseEntity<ResponseData> changePassword(@CurrentUser UserPrincipal userPrincipal,
                                                       @Valid @RequestBody PasswordChangingRequest request,
                                                       HttpServletRequest httpServletRequest) {
        authService.changePassword(userPrincipal.getId(), request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.PASSWORD_CHANGED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping(ApiPath.REFRESH_TOKEN)
    public ResponseEntity<ResponseData> refreshJwt(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest,
                                                   HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(jwtService.refreshToken(refreshTokenRequest.getRefreshToken()),
                FeedbackMessage.TOKEN_REFRESHED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

}
