package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.payload.response.UserEmailResponse;
import com.pbl6.cinemate.auth_service.service.AccountService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final AccountService accountService;
    
    @GetMapping("/search")
    public ResponseEntity<ResponseData> searchUsersByEmail(
            @RequestParam(name = "email") String searchText,
            HttpServletRequest httpServletRequest) {
        List<UserEmailResponse> results = accountService.searchUsersByEmail(searchText);
        String message = results.isEmpty() 
                ? String.format("No users found matching '%s'", searchText)
                : String.format("Found %d user(s) matching '%s'", results.size(), searchText);
        return ResponseEntity.ok(ResponseData.success(
                results,
                message,
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
