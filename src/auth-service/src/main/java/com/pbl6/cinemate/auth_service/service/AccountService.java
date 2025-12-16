package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.payload.request.AdminResetPasswordRequest;
import com.pbl6.cinemate.auth_service.payload.request.CreateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.request.UpdateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.response.UserEmailResponse;
import com.pbl6.cinemate.auth_service.payload.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    UserResponse createAccount(CreateAccountRequest request);

    UserResponse getAccountById(UUID accountId);
    
    UserEmailResponse getEmailByUserId(UUID userId);

    List<UserResponse> getAllAccounts();

    UserResponse updateAccount(UUID accountId, UpdateAccountRequest request);

    void deleteAccount(UUID accountId, UUID currentUserId);

    void banAccount(UUID accountId, UUID currentUserId);

    void unlockAccount(UUID accountId);

    void resetAccountPassword(UUID accountId, AdminResetPasswordRequest request);

    List<UserResponse> getAccountsByRole(UUID roleId);

    List<UserResponse> getAccountsByStatus(Boolean isEnabled);
}
