package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;

import com.pbl6.cinemate.auth_service.payload.request.AdminResetPasswordRequest;
import com.pbl6.cinemate.auth_service.payload.request.CreateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.request.UpdateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.response.UserResponse;

import com.pbl6.cinemate.auth_service.service.AccountService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.ACCOUNTS)
@RequiredArgsConstructor
public class AccountController {

        private final AccountService accountService;

        // Create new account (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping
        public ResponseEntity<ResponseData> createAccount(
                        @RequestBody @Valid CreateAccountRequest request,
                        HttpServletRequest httpServletRequest) {
                UserResponse response = accountService.createAccount(request);
                return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ACCOUNT_CREATED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Get all accounts (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping
        public ResponseEntity<ResponseData> getAllAccounts(HttpServletRequest httpServletRequest) {
                List<UserResponse> responses = accountService.getAllAccounts();
                return ResponseEntity.ok(ResponseData.success(responses, FeedbackMessage.ACCOUNTS_FETCHED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Get account by ID (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/{id}")
        public ResponseEntity<ResponseData> getAccountById(
                        @PathVariable UUID id,
                        HttpServletRequest httpServletRequest) {
                UserResponse response = accountService.getAccountById(id);
                return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ACCOUNT_FETCHED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Update account (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @PutMapping("/{id}")
        public ResponseEntity<ResponseData> updateAccount(
                        @PathVariable UUID id,
                        @RequestBody @Valid UpdateAccountRequest request,
                        HttpServletRequest httpServletRequest) {
                UserResponse response = accountService.updateAccount(id, request);
                return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ACCOUNT_UPDATED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Delete account (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @DeleteMapping("/{id}")
        public ResponseEntity<ResponseData> deleteAccount(
                        @PathVariable UUID id,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {
                accountService.deleteAccount(id, userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                                FeedbackMessage.ACCOUNT_DELETED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Ban account (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @PatchMapping("/{id}/ban")
        public ResponseEntity<ResponseData> banAccount(
                        @PathVariable UUID id,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {
                accountService.banAccount(id, userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                                FeedbackMessage.ACCOUNT_BANNED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Unlock account (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @PatchMapping("/{id}/unlock")
        public ResponseEntity<ResponseData> unlockAccount(
                        @PathVariable UUID id,
                        HttpServletRequest httpServletRequest) {
                accountService.unlockAccount(id);
                return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                                FeedbackMessage.ACCOUNT_UNLOCKED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Reset account password (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @PatchMapping("/{id}/reset-password")
        public ResponseEntity<ResponseData> resetAccountPassword(
                        @PathVariable UUID id,
                        @RequestBody @Valid AdminResetPasswordRequest request,
                        HttpServletRequest httpServletRequest) {
                accountService.resetAccountPassword(id, request);
                return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                                FeedbackMessage.ACCOUNT_PASSWORD_RESET,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Get accounts by role (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/role/{roleId}")
        public ResponseEntity<ResponseData> getAccountsByRole(
                        @PathVariable UUID roleId,
                        HttpServletRequest httpServletRequest) {
                List<UserResponse> responses = accountService.getAccountsByRole(roleId);
                return ResponseEntity.ok(ResponseData.success(responses, FeedbackMessage.ACCOUNTS_FETCHED_BY_ROLE,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }

        // Get accounts by status (Admin only)
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/status")
        public ResponseEntity<ResponseData> getAccountsByStatus(
                        @RequestParam(name = "is_enabled") Boolean isEnabled,
                        HttpServletRequest httpServletRequest) {
                List<UserResponse> responses = accountService.getAccountsByStatus(isEnabled);
                return ResponseEntity.ok(ResponseData.success(responses, FeedbackMessage.ACCOUNTS_FETCHED,
                                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
        }
}
