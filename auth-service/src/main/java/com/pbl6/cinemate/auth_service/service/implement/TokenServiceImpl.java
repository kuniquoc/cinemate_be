package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.constant.TokenExpirationTime;
import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.enums.TokenType;
import com.pbl6.cinemate.auth_service.exception.InternalServerException;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.repository.TokenRepository;
import com.pbl6.cinemate.auth_service.service.TokenService;
import com.pbl6.cinemate.auth_service.utils.CommonUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final TokenRepository tokenRepository;

    @Override
    public Token createToken(User user, TokenType type) {
        switch (type) {
            case ACCOUNT_VERIFICATION -> {
                Token token = new Token();
                token.setType(TokenType.ACCOUNT_VERIFICATION);
                token.setContent(UUID.randomUUID().toString());
                token.setExpireTime(LocalDateTime.now().plusHours(TokenExpirationTime.VERIFY_ACCOUNT_TOKEN_HOURS));
                token.setUser(user);
                return tokenRepository.save(token);
            }
            case RESET_PASSWORD -> {
                Token token = new Token();
                token.setType(TokenType.RESET_PASSWORD);
                token.setContent(String.valueOf(CommonUtils.getRandomFourDigitNumber()));
                token.setExpireTime(LocalDateTime.now().plusMinutes(TokenExpirationTime.RESET_PASSWORD_TOKEN_MINUTES));
                token.setUser(user);
                return tokenRepository.save(token);
            }
            case DELETE_ACCOUNT -> {
                Token token = new Token();
                token.setType(TokenType.DELETE_ACCOUNT);
                token.setContent(UUID.randomUUID().toString());
                token.setExpireTime(LocalDateTime.now().plusMinutes(TokenExpirationTime.DELETE_ACCOUNT_TOKEN_MINUTES));
                token.setUser(user);
                return tokenRepository.save(token);
            }
            default -> throw new InternalServerException(ErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Token findTokenByContent(String content) {
        return tokenRepository.findByContent(content)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.TOKEN_NOT_FOUND));
    }

    @Transactional
    @Override
    public void deleteTokenByContent(String content) {
        tokenRepository.deleteByContent(content);
    }

    @Override
    public Optional<Token> findOtp(UUID userId) {
        return tokenRepository.findByTypeAndUserId(TokenType.RESET_PASSWORD, userId);
    }

    @Override
    public Token save(Token token) {
        return tokenRepository.save(token);
    }

    @Override
    public Token findByContentAndUserId(String contentToken, UUID userId) {
        return tokenRepository.findByContentAndUserId(contentToken, userId).orElseThrow(()
                -> new NotFoundException(ErrorMessage.TOKEN_NOT_FOUND));
    }
}
