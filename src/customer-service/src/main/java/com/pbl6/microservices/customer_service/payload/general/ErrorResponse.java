package com.pbl6.microservices.customer_service.payload.general;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    String message;
    String field;
    String code;

    public ErrorResponse(String code, String message, String field) {
        this.message = message;
        this.field = field;
        this.code = code;
    }

    public ErrorResponse(String code, String message) {
        this.message = message;
        this.code = code;
    }
}
