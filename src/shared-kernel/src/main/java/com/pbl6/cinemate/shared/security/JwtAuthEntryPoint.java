package com.pbl6.cinemate.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.shared.dto.general.ErrorResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.utils.CommonUtils;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String authHeader = request.getHeader("Authorization");

        ErrorResponse error;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            error = new ErrorResponse( "AUTH0001", "missing jwt");
        } else {
            error = new ErrorResponse("AUTH0002", "Unauthenticated");
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ResponseData responseData = ResponseData.error(error, request.getRequestURI(), request.getMethod());

        log.error("Unauthorized request {} {}: {}", request.getMethod(), request.getRequestURL(), authException.getMessage());

        response.getWriter().write(Objects.requireNonNull(CommonUtils.toJsonString(responseData)));
    }
}