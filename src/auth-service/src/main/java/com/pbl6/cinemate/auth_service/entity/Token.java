package com.pbl6.cinemate.auth_service.entity;

import com.pbl6.cinemate.auth_service.enums.TokenType;
import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "tokens")
public class Token extends AbstractBaseEntity {
    String content;

    @Enumerated(EnumType.STRING)
    TokenType type;
    Instant expireTime;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}