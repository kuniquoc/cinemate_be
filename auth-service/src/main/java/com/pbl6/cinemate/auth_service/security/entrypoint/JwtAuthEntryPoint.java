package com.pbl6.cinemate.auth_service.security.entrypoint;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.payload.general.ErrorResponse;
import com.pbl6.cinemate.auth_service.payload.general.ResponseData;
import com.pbl6.cinemate.auth_service.utils.CommonUtils;
import com.pbl6.cinemate.auth_service.utils.ErrorUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

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
            error = ErrorUtils.getExceptionError(ErrorMessage.MISSING_JWT);
        } else {
            error = ErrorUtils.getExceptionError(ErrorMessage.UNAUTHENTICATED);
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ResponseData responseData = ResponseData.error(error);

        log.error("Unauthorized request {} {}: {}", request.getMethod(), request.getRequestURL(), authException.getMessage());

        response.getWriter().write(Objects.requireNonNull(CommonUtils.toJsonString(responseData)));
    }
}