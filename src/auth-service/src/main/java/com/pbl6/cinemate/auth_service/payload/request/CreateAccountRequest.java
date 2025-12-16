package com.pbl6.cinemate.auth_service.payload.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.pbl6.cinemate.shared.constants.CommonConstant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAccountRequest {
    @NotBlank
    @Email
    String email;

    @NotBlank
    @Pattern(regexp = CommonConstant.PASSWORD_RULE)
    String password;

    @NotBlank
    String firstName;

    @NotBlank
    String lastName;

    UUID roleId;
}
