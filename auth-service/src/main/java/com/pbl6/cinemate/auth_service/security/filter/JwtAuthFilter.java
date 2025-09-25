package com.pbl6.cinemate.auth_service.security.filter;


import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.exception.UnauthenticatedException;
import com.pbl6.cinemate.auth_service.payload.general.ErrorResponse;
import com.pbl6.cinemate.auth_service.payload.general.ResponseData;
import com.pbl6.cinemate.auth_service.service.implement.CustomUserDetailsService;
import com.pbl6.cinemate.auth_service.utils.CommonUtils;
import com.pbl6.cinemate.auth_service.utils.ErrorUtils;
import com.pbl6.cinemate.auth_service.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = parseJwt(request);

        try {
            if (StringUtils.hasText(token)) {
                Claims claims = jwtUtils.verifyToken(token, false);
                UserDetails userDetails = customUserDetailsService
                        .loadUserByUsername(jwtUtils.getUsernameFromJWTClaims(claims));

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (UnauthenticatedException ex) {
            log.error("Auth failed for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            String errorMessage = ex.getMessage();
            ErrorResponse error = ErrorUtils.getExceptionError(errorMessage);
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, error);

        } catch (Exception ex) {
            log.error("Internal error for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            writeErrorResponse(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ErrorUtils.getExceptionError(ErrorMessage.INTERNAL_SERVER_ERROR));
        }
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    int status,
                                    ErrorResponse error) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ResponseData responseData = ResponseData.error(error);
        response.getWriter().write(Objects.requireNonNull(CommonUtils.toJsonString(responseData)));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        return (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer "))
                ? headerAuth.substring(7)
                : null;
    }
}
