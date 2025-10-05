package com.pbl6.cinemate.movie.dto.general;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResponseData {
    String status;
    Object data;
    Object errors;
    Object meta;
    String message;
    String title;
    String detail;
    String path;
    String method;
    Long timestamp;

    public static ResponseData success(Object data, String message, String apiPath, String method) {
        return ResponseData.builder()
                .status("success")
                .message(message)
                .path(apiPath)
                .method(method)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ResponseData error(Object error, String apiPath, String method) {
        ErrorResponse errorResponse = (ErrorResponse) error;
        return ResponseData.builder()
                .status("error")
                .title("Error: " + errorResponse.getCode())
                .detail(errorResponse.getMessage())
                .path(apiPath)
                .method(method)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ResponseData valError(Object error, String apiPath, String method) {
        @SuppressWarnings("unchecked")
        List<ErrorResponse> errorResponses = (List<ErrorResponse>) error;
        return ResponseData.builder()
                .status("error")
                .title("Validation Error")
                .detail("One or more validation errors occurred")
                .path(apiPath)
                .method(method)
                .errors(errorResponses)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}