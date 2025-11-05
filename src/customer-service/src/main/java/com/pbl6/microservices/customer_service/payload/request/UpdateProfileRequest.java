package com.pbl6.microservices.customer_service.payload.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;


@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;      // "MALE", "FEMALE", "OTHER"
    private String displayLang; // ví dụ: "en", "vi"
}