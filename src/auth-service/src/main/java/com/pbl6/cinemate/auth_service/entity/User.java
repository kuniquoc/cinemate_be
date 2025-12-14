package com.pbl6.cinemate.auth_service.entity;

import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends AbstractBaseEntity {

    @Column(nullable = false)
    String firstName;
    @Column(nullable = false)
    String lastName;
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    Boolean isEnabled = false;
    Instant accountVerifiedAt;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(nullable = false, length = 255)
    private String password;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
