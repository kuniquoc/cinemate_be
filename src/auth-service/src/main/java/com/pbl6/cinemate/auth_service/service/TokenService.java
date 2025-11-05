package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.enums.TokenType;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface TokenService {
    Token createToken(User user, TokenType type);

    Token findTokenByContent(String content);

    @Transactional
    void deleteTokenByContent(String content);

    Optional<Token> findOtp(UUID userId);

    Token save(Token token);

    Token findByContentAndUserId(String content, UUID userId);
}
