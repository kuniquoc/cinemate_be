package com.pbl6.cinemate.auth_service.payload.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtLoginResponse extends LoginResponse {
    String accessToken;
    String refreshToken;

    public JwtLoginResponse(UserResponse response, String accessToken, String refreshToken) {
        super(response);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
