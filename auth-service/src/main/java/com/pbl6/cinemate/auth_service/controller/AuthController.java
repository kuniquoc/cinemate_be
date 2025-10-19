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
@RequestMapping(ApiPath.AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/sign-up")
    public ResponseEntity<ResponseData> signUp(@Valid @RequestBody SignUpRequest signUpRequest,
                                               HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(authService.signUp(signUpRequest),
                FeedbackMessage.SIGNED_UP, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ResponseData> verifyEmail(@Valid @RequestBody VerifyEmailRequest verifyEmailRequest,
                                                    HttpServletRequest request) {
        authService.verifyEmail(verifyEmailRequest);
        ResponseData responseData = ResponseData
                .successWithoutMetaAndData(FeedbackMessage.EMAIL_SENT, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/verify-token")
    public ResponseEntity<ResponseData> verifyToken(@Valid @RequestBody VerifyTokenRequest verifyTokenRequest,
                                                    HttpServletRequest request) {
        ResponseData responseData = ResponseData
                .success(authService.verifyToken(verifyTokenRequest),
                        FeedbackMessage.EMAIL_VERIFIED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseData> login(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest httpServletRequest) {
        ResponseData responseData = ResponseData.success(authService.login(loginRequest),
                FeedbackMessage.LOGGED_IN, httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseData> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
                                                             HttpServletRequest request) {
        authService.forgotPassword(forgotPasswordRequest);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.FORGOT_PASSWORD,
                request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseData> verifyOTP(@Valid @RequestBody OtpVerificationRequest request,
                                                  HttpServletRequest httpServletRequest) {
        ResponseData responseData = ResponseData.success(authService.verifyOTP(request.getEmail(), request.getOtp()),
                FeedbackMessage.OTP_VERIFIED, httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseData> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                      HttpServletRequest httpServletRequest) {
        authService.resetPassword(request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.RESET_PASSWORD,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/log-out")
    public ResponseEntity<ResponseData> logout(@Valid @RequestBody LogoutRequest logoutRequest, HttpServletRequest request) {
        authService.logout(logoutRequest);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.LOGGED_OUT,
                request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ResponseData> changePassword(@CurrentUser UserPrincipal userPrincipal,
                                                       @Valid @RequestBody PasswordChangingRequest request,
                                                       HttpServletRequest httpServletRequest) {
        authService.changePassword(userPrincipal.getId(), request);
        ResponseData responseData = ResponseData.successWithoutMetaAndData(FeedbackMessage.PASSWORD_CHANGED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ResponseData> refreshJwt(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest,
                                                   HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(jwtService.refreshToken(refreshTokenRequest.getRefreshToken()),
                FeedbackMessage.TOKEN_REFRESHED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/verify-jwt")
    public ResponseEntity<ResponseData> verifyJwt(@RequestBody @Valid JwtVerificationRequest jwtVerificationRequest,
                                                  HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(jwtService.isTokenValid(jwtVerificationRequest.getToken()),
                FeedbackMessage.TOKEN_VERIFIED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }
}
