package com.pbl6.cinemate.auth_service.event.handler;

import com.pbl6.cinemate.auth_service.constant.CommonConstant;
import com.pbl6.cinemate.auth_service.constant.TokenExpirationTime;
import com.pbl6.cinemate.auth_service.email.EmailService;
import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.enums.TokenType;
import com.pbl6.cinemate.auth_service.event.ForgotPasswordEvent;
import com.pbl6.cinemate.auth_service.service.TokenService;
import com.pbl6.cinemate.auth_service.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForgotPasswordHandler implements ApplicationListener<ForgotPasswordEvent> {
    private final TokenService tokenService;
    private final EmailService emailService;

    @Async("taskExecutor")
    @Override
    public void onApplicationEvent(ForgotPasswordEvent event) {
        User user = event.getUser();

        Optional<Token> tokenResult = tokenService.findOtp(user.getId());
        Token token;
        if (tokenResult.isPresent()) {
            token = tokenResult.get();
            token.setContent(String.valueOf(CommonUtils.getRandomFourDigitNumber()));
            token.setExpireTime(LocalDateTime.now().plusMinutes(TokenExpirationTime.RESET_PASSWORD_TOKEN_MINUTES));
            tokenService.save(token);
        } else {
            token = tokenService.createToken(user, TokenType.RESET_PASSWORD);
        }

        emailService.sendMailForgetPassword(user.getEmail(), token.getContent(), CommonConstant.LANGUAGE_CODE);
    }
}
