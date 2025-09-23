package com.pbl6.cinemate.auth_service.repository;

import com.pbl6.cinemate.auth_service.entity.Token;
import com.pbl6.cinemate.auth_service.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByContent(String content);

    void deleteByContent(String content);

    Optional<Token> findByTypeAndUserId(TokenType type, UUID userId);

    Optional<Token> findByContentAndUserId(String content, UUID userId);

    void deleteTokenByContent(String tokenContent);

}
