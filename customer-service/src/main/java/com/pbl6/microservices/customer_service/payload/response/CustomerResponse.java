package com.pbl6.microservices.customer_service.payload.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerResponse {
    private UUID id;
    private UUID accountId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;       // "MALE", "FEMALE", "OTHER"
    private String displayLang;  // ví dụ: "en", "vi"
    private Boolean isAnonymous;
}
