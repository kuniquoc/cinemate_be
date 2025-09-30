package com.pbl6.cinemate.auth_service.payload.general;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pbl6.cinemate.auth_service.constant.CommonConstant;
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
    String code;

    public static ResponseData successWithMeta(Object data, Object meta, String message) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).data(data).meta(meta).message(message).build();
    }

    public static ResponseData success(Object data, String message, String apiPath, String method) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).message(message).path(apiPath).method(method)
                .data(data).build();
    }

    public static ResponseData successWithoutMetaAndData(String message, String apiPath, String method) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).message(message).path(apiPath)
                .method(method).build();
    }

    public static ResponseData error(Object error, String apiPath, String method) {
        ErrorResponse errorResponse = (ErrorResponse) error;
        return ResponseData.builder().status(CommonConstant.FAILURE)
                .code(errorResponse.getCode())
                .title("Error Occurred ")
                .detail(errorResponse.getMessage())
                .path(apiPath)
                .method(method)
                .build();
    }

    public static ResponseData valError(Object error, String apiPath, String method) {
        List<ErrorResponse> errorResponses = (List<ErrorResponse>) error;
        return ResponseData.builder().status(CommonConstant.FAILURE)
                .title("Validation Error")
                .detail("One or more validation errors occurred")
                .path(apiPath)
                .method(method)
                .errors(errorResponses).build();
    }
}