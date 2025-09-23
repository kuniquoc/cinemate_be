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

    public static ResponseData successWithMeta(Object data, Object meta, String message) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).data(data).meta(meta).message(message).build();
    }

    public static ResponseData success(Object data, String message) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).data(data).message(message).build();
    }

    public static ResponseData successWithoutMetaAndData(String message) {
        return ResponseData.builder().status(CommonConstant.SUCCESS).message(message).build();
    }

    public static ResponseData error(Object error) {
        ErrorResponse errorResponse = (ErrorResponse) error;
        return ResponseData.builder().status(CommonConstant.FAILURE)
                .message(errorResponse.getMessage())
                .errors(error).build();
    }

    public static ResponseData valError(Object error) {
        List<ErrorResponse> errorResponses = (List<ErrorResponse>) error;
        return ResponseData.builder().status(CommonConstant.FAILURE)
                .message("Validation Error")
                .errors(errorResponses).build();
    }
}