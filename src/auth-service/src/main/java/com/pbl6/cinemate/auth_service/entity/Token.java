package com.pbl6.cinemate.auth_service.entity;


import com.pbl6.cinemate.auth_service.enums.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tokens")
public class Token extends AbstractEntity {
    @Id
    @GeneratedValue
    UUID id;
    String content;

    @Enumerated(EnumType.STRING)
    TokenType type;
    LocalDateTime expireTime;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}