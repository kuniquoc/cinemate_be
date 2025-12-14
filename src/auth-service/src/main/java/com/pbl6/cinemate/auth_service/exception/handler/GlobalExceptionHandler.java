package com.pbl6.cinemate.auth_service.exception.handler;

import com.pbl6.cinemate.auth_service.utils.ErrorUtils;
import com.pbl6.cinemate.shared.constants.ErrorMessage;
import com.pbl6.cinemate.shared.dto.general.ErrorResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.InternalServerException;
import com.pbl6.cinemate.shared.exception.NotFoundException;
import com.pbl6.cinemate.shared.exception.UnauthenticatedException;
import com.pbl6.cinemate.shared.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseData> handlingBadRequestException(BadRequestException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseData> handlingUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ResponseData> handlingUnauthenticatedException(UnauthenticatedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseData> handlingNotFoundException(NotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ResponseData> handlingInternalServerException(InternalServerException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseData> handlingMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                                                HttpServletRequest request) {
        List<ObjectError> errors = ex.getBindingResult().getAllErrors();
        List<ErrorResponse> errorResponses = new ArrayList<>();
        errors.stream().forEach(objectError -> {
            String error = ErrorUtils.convertToSnakeCase(Objects.requireNonNull(objectError.getCode()));
            String fieldName = ErrorUtils.convertToSnakeCase(((FieldError) objectError).getField());
            String resource = ErrorUtils.convertToSnakeCase(objectError.getObjectName());

            log.debug("Validation error - Resource: {}, Field: {}, Error: {}", resource, fieldName, error);

            ErrorResponse errorResponse = ErrorUtils.getValidationError(resource, fieldName, error);
            errorResponses.add(errorResponse);
        });

        ResponseData responseData = ResponseData.valError(errorResponses, request.getRequestURI(), request.getMethod());

        return new ResponseEntity<>(responseData, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseData> handlingAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorUtils.getExceptionError(ErrorMessage.UNAUTHORIZED);
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<ResponseData> handlingClassCastException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .message(ErrorMessage.WRONG_FORMAT)
                .build();
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData> handlingException(Exception ex, HttpServletRequest request) {
        log.error(ex.getMessage());
        log.error(ex.getClass().getName());
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .build();
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}