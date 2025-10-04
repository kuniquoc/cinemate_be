package com.pbl6.microservices.customer_service.entity;


import com.pbl6.microservices.customer_service.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends AbstractEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "account_id", nullable = false, columnDefinition = "UUID")
    private UUID accountId; // mapping sang auth-service.users.id

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender = Gender.OTHER;

    @Column(name = "display_lang", length = 10)
    private String displayLang = "en";

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false;
}