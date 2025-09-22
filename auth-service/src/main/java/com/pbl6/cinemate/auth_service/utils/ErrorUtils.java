package com.pbl6.cinemate.auth_service.utils;

import com.pbl6.cinemate.auth_service.payload.general.ErrorResponse;

import java.util.Map;

public final class ErrorUtils {
    private static final String ERROR_FILE = "errors.yml";
    private static final String VALIDATION_FILE = "validations.yml";

    private ErrorUtils() {
    }

    @SuppressWarnings("unchecked")
    public static ErrorResponse getExceptionError(String error) {
        Map<String, Object> errors = ReadYAML.getValueFromYAML(ERROR_FILE);
        Map<String, Object> objError = (Map<String, Object>) errors.get(error);

        String code = (String) objError.get("code");
        String message = (String) objError.get("message");

        return new ErrorResponse(code, message);
    }

    @SuppressWarnings("unchecked")
    public static ErrorResponse getValidationError(String resource, String fieldName, String error) {
        if (fieldName.contains("[")) {
            fieldName = handleFieldName(fieldName);
        }

        Map<String, Object> errors = ReadYAML.getValueFromYAML(VALIDATION_FILE);
        Map<String, Object> fields = (Map<String, Object>) errors.get(resource);
        Map<String, Object> objErrors = (Map<String, Object>) fields.get(fieldName);
        Map<String, Object> objError = (Map<String, Object>) objErrors.get(error);

        String code = (String) objError.get("code");
        String message = (String) objError.get("message");

        return new ErrorResponse(code, message, fieldName);
    }

    public static String handleFieldName(String fieldName) {
        String index = fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]"));
        return fieldName.replaceAll(index, "");
    }

    public static String convertToSnakeCase(String input) {
        return input.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase();
    }
}
