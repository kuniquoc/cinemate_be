package com.pbl6.cinemate.shared.security;

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

import com.pbl6.cinemate.shared.dto.general.ErrorResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.exception.UnauthenticatedException;
import com.pbl6.cinemate.shared.utils.CommonUtils;
import com.pbl6.cinemate.shared.utils.JwtUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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
                Claims claims = jwtUtils.verifyToken(token, false);
                String userId = claims.get("user_id", String.class);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                List<String> permissions = claims.get("permissions", List.class);
                String firstName = claims.get("first_name", String.class);
                String lastName = claims.get("last_name", String.class);
                UserDetails userDetails = UserPrincipal.createUserPrincipal(userId, username, null, role, permissions,
                        firstName, lastName);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);

        } catch (UnauthenticatedException ex) {
            log.error("Auth failed for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            String errorMessage = ex.getMessage();
            ErrorResponse error = new ErrorResponse("AUTH0001", errorMessage);
            writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, error);

        } catch (Exception ex) {
            log.error("Internal error for {} {}: {}", request.getMethod(), request.getRequestURL(), ex.getMessage());
            writeErrorResponse(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new ErrorResponse("INTERNAL", "Internal server error"));
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
