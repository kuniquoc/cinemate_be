package com.pbl6.cinemate.auth_service.event.handler;

import com.pbl6.cinemate.auth_service.constant.CommonConstant;
import com.pbl6.cinemate.auth_service.email.EmailService;
import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.enums.TokenType;
import com.pbl6.cinemate.auth_service.event.UserRegistrationEvent;
import com.pbl6.cinemate.auth_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener implements ApplicationListener<UserRegistrationEvent> {
    private final EmailService emailService;
    private final TokenService tokenService;

    @Async("taskExecutor")
    @Override
    public void onApplicationEvent(@NonNull UserRegistrationEvent event) {
        log.info("Verification email event received for user: {}", event.getUser().getEmail());
        User user = event.getUser();
        Token token = tokenService.createToken(user, TokenType.ACCOUNT_VERIFICATION);
        log.info("Verification token created: {}", token.getContent());

        emailService.sendMailConfirmRegister("User ", user.getEmail(), user.getEmail(),
                token.getContent(), CommonConstant.LANGUAGE_CODE);
    }
}