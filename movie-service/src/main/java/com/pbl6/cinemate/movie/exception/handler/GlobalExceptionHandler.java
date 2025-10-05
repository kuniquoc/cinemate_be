package com.pbl6.cinemate.movie.exception.handler;

import com.pbl6.cinemate.movie.dto.general.ErrorResponse;
import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseData> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.error("BadRequestException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseData> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        log.error("NotFoundException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ResponseData> handleInternalServerException(InternalServerException ex,
            HttpServletRequest request) {
        log.error("InternalServerException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ResponseData> handleUnauthenticatedException(UnauthenticatedException ex,
            HttpServletRequest request) {
        log.error("UnauthenticatedException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("UNAUTHORIZED", ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseData> handleUnauthorizedException(UnauthorizedException ex,
            HttpServletRequest request) {
        log.error("UnauthorizedException: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("FORBIDDEN", ex.getMessage());
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());
        return new ResponseEntity<>(responseData, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}