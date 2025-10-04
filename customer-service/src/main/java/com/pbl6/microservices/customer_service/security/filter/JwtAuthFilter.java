package com.pbl6.microservices.customer_service.security.filter;


import com.pbl6.microservices.customer_service.constants.ErrorMessage;
import com.pbl6.microservices.customer_service.exception.UnauthenticatedException;
import com.pbl6.microservices.customer_service.payload.general.ErrorResponse;
import com.pbl6.microservices.customer_service.payload.general.ResponseData;
import com.pbl6.microservices.customer_service.security.UserPrincipal;
import com.pbl6.microservices.customer_service.utils.CommonUtils;
import com.pbl6.microservices.customer_service.utils.ErrorUtils;
import com.pbl6.microservices.customer_service.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = parseJwt(request);

        try {
            if (StringUtils.hasText(token)) {
                Claims claims = jwtUtils.verifyToken(token);
                String username = jwtUtils.getUsernameFromJWTClaims(claims); // "sub" trong JWT
                String role = claims.get("role", String.class);
                String userIdStr = claims.get("user_id", String.class);
                UUID userId = UUID.fromString(userIdStr);

                Collection<GrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                );
                UserDetails userDetails = new UserPrincipal(userId, username, authorities);
                var authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (UnauthenticatedException ex) {
            log.error("Auth failed for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            String errorMessage = ex.getMessage();
            ErrorResponse error = ErrorUtils.getExceptionError(errorMessage);
            writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, error);

        } catch (Exception ex) {
            log.error("Internal error for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            writeErrorResponse(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ErrorUtils.getExceptionError(ErrorMessage.INTERNAL_SERVER_ERROR));
        }
    }

    private void writeErrorResponse(HttpServletRequest reques, HttpServletResponse response,
                                    int status,
                                    ErrorResponse error) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ResponseData responseData = ResponseData.error(error, reques.getRequestURI(), reques.getMethod());
        response.getWriter().write(Objects.requireNonNull(CommonUtils.toJsonString(responseData)));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        return (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer "))
                ? headerAuth.substring(7)
                : null;
    }
}
