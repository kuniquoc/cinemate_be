package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.exception.BadRequestException;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.mapper.UserMapper;
import com.pbl6.cinemate.auth_service.payload.request.AdminResetPasswordRequest;
import com.pbl6.cinemate.auth_service.payload.request.CreateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.request.UpdateAccountRequest;
import com.pbl6.cinemate.auth_service.payload.response.UserResponse;
import com.pbl6.cinemate.auth_service.repository.RoleRepository;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.auth_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createAccount(CreateAccountRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(ErrorMessage.USER_ALREADY_EXISTED);
        }

        // Get role
        Role role = null;
        if (request.getRoleId() != null) {
            role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .isEnabled(true)
                .accountVerifiedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        return UserMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse getAccountById(UUID accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
        return UserMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllAccounts() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(UserMapper::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateAccount(UUID accountId, UpdateAccountRequest request) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException(ErrorMessage.USER_ALREADY_EXISTED);
            }
            user.setEmail(request.getEmail());
        }

        // Update first name if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        // Update last name if provided
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        // Update role if provided
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
            user.setRole(role);
        }

        User updatedUser = userRepository.save(user);
        return UserMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountId, UUID currentUserId) {
        // Prevent deleting own account
        if (accountId.equals(currentUserId)) {
            throw new BadRequestException(ErrorMessage.CANNOT_DELETE_OWN_ACCOUNT);
        }

        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // Soft delete by setting deletedAt
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void banAccount(UUID accountId, UUID currentUserId) {
        // Prevent banning own account
        if (accountId.equals(currentUserId)) {
            throw new BadRequestException(ErrorMessage.CANNOT_BAN_OWN_ACCOUNT);
        }

        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // Check if account is already banned
        if (!user.getIsEnabled()) {
            throw new BadRequestException(ErrorMessage.ACCOUNT_ALREADY_BANNED);
        }

        user.setIsEnabled(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unlockAccount(UUID accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        // Check if account is already unlocked
        if (user.getIsEnabled()) {
            throw new BadRequestException(ErrorMessage.ACCOUNT_ALREADY_UNLOCKED);
        }

        user.setIsEnabled(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetAccountPassword(UUID accountId, AdminResetPasswordRequest request) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<UserResponse> getAccountsByRole(UUID roleId) {
        // Verify role exists
        roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));

        return userRepository.findByRoleId(roleId)
                .stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(UserMapper::toUserResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getAccountsByStatus(Boolean isEnabled) {
        return userRepository.findAllByIsEnabled(isEnabled)
                .stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(UserMapper::toUserResponse)
                .toList();
    }
}
