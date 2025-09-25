package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.constant.RoleName;
import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.entity.UserPrincipal;
import com.pbl6.cinemate.auth_service.enums.CachePrefix;
import com.pbl6.cinemate.auth_service.event.ForgotPasswordEvent;
import com.pbl6.cinemate.auth_service.event.UserRegistrationEvent;
import com.pbl6.cinemate.auth_service.exception.BadRequestException;
import com.pbl6.cinemate.auth_service.exception.UnauthenticatedException;
import com.pbl6.cinemate.auth_service.mapper.UserMapper;
import com.pbl6.cinemate.auth_service.payload.request.*;
import com.pbl6.cinemate.auth_service.payload.response.JwtLoginResponse;
import com.pbl6.cinemate.auth_service.payload.response.LoginResponse;
import com.pbl6.cinemate.auth_service.payload.response.SignUpResponse;
import com.pbl6.cinemate.auth_service.service.*;
import com.pbl6.cinemate.auth_service.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CacheService cacheService;

    @Override
    public SignUpResponse signUp(SignUpRequest signUpRequest) {
        if (!signUpRequest.getPassword().equals(signUpRequest.getPasswordConfirmation())) {
            throw new BadRequestException(ErrorMessage.PASSWORD_CONFIRM_PASSWORD_NOT_MATCHED);
        }

        if (userService.isExistedUser(signUpRequest.getEmail())) {
            throw new BadRequestException(ErrorMessage.USER_ALREADY_EXISTED);
        }

        Role userRole = roleService.findByName(RoleName.USER);

        User user = UserMapper.toUser(signUpRequest);
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(userRole);
        User savedUser = userService.save(user);

        eventPublisher.publishEvent(new UserRegistrationEvent(savedUser));

        return new SignUpResponse(UserMapper.toUserResponse(savedUser));
    }

    @Transactional
    @Override
    public void verifyAccount(AccountVerificationRequest accountVerificationRequest) {
        User user = userService.findByToken(accountVerificationRequest.getAccountVerificationToken());
        if (user.getAccountVerifiedAt() != null) {
            throw new BadRequestException(ErrorMessage.ACCOUNT_ALREADY_ACTIVE);
        }
        user.setIsEnabled(true);
        user.setAccountVerifiedAt(LocalDateTime.now());
        tokenService.deleteTokenByContent(accountVerificationRequest.getAccountVerificationToken());
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            boolean isRefreshToken = true;
            String refreshToken = jwtUtils.generateToken(userPrincipal, isRefreshToken);
            String accessToken = jwtUtils.generateToken(userPrincipal, !isRefreshToken);

            return new JwtLoginResponse(UserMapper.toUserResponse(userService.findById(userPrincipal.getId())),
                    accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            throw new BadRequestException(ErrorMessage.INVALID_EMAIL_OR_PASSWORD);
        } catch (InternalAuthenticationServiceException e) {
            throw new UnauthenticatedException(ErrorMessage.ACCOUNT_NOT_EXISTED);
        } catch (DisabledException e) {
            User user = userService.findByEmail(loginRequest.getEmail());
            if (user.getAccountVerifiedAt() != null) {
                throw new UnauthenticatedException(ErrorMessage.ACCOUNT_LOCKED);
            } else {
                throw new UnauthenticatedException(ErrorMessage.ACCOUNT_NOT_ACTIVE);
            }
        } catch (AuthenticationException e) {
            throw new UnauthenticatedException(ErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail());
        eventPublisher.publishEvent(new ForgotPasswordEvent(user));
    }

    @Override
    public boolean verifyOTP(String email, String contentToken) {
        User user = userService.findByEmail(email);
        Token tokenEntity = tokenService.findByContentAndUserId(contentToken, user.getId());
        if (tokenEntity == null)
            throw new BadRequestException(ErrorMessage.INVALID_OTP);
        if (tokenEntity.getExpireTime().isBefore(LocalDateTime.now()))
            throw new BadRequestException(ErrorMessage.EXPIRED_OTP);
        return true;
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userService.findByEmail(request.getEmail());
        if (verifyOTP(request.getEmail(), request.getOtp())) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userService.save(user);
            tokenService.deleteTokenByContent(request.getOtp());
        } else throw new BadRequestException(ErrorMessage.INVALID_OTP);
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        String refreshToken = logoutRequest.getRefreshToken();

        boolean isRefreshToken = true;
        Claims refreshTokenClaims = jwtUtils.verifyToken(refreshToken, isRefreshToken);

        String prefix = CachePrefix.BLACK_LIST_TOKENS.getPrefix();
        log.info(prefix);
        cacheService.set(prefix + jwtUtils.getJwtIdFromJWTClaims(refreshTokenClaims), 1,
                jwtUtils.getTokenAvailableDuration(refreshTokenClaims), TimeUnit.MILLISECONDS);
        log.info(jwtUtils.getJwtIdFromJWTClaims(refreshTokenClaims));
        log.info(jwtUtils.getJwtIdFromJWTClaims(refreshTokenClaims));
    }

    @Override
    public void changePassword(UUID userId, PasswordChangingRequest passwordChangingRequest) {
        if (!Objects.equals(passwordChangingRequest.getNewPassword(), passwordChangingRequest.getNewPasswordConfirmation()))
            throw new BadRequestException(ErrorMessage.NEW_PASSWORD_NOT_MATCHED);

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(passwordChangingRequest.getOldPassword(), user.getPassword()))
            throw new BadRequestException(ErrorMessage.OLD_PASSWORD_NOT_MATCHED);
        user.setPassword(passwordEncoder.encode(passwordChangingRequest.getNewPassword()));
        userService.save(user);
    }
}
