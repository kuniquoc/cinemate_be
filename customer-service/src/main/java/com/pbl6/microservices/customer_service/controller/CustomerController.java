package com.pbl6.microservices.customer_service.controller;

import com.pbl6.microservices.customer_service.constants.ApiPath;
import com.pbl6.microservices.customer_service.constants.FeedbackMessage;
import com.pbl6.microservices.customer_service.payload.general.ResponseData;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.security.UserPrincipal;
import com.pbl6.microservices.customer_service.security.annotation.CurrentUser;
import com.pbl6.microservices.customer_service.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PatchMapping(ApiPath.UPDATE_PROFILE)
    public ResponseEntity<ResponseData> updateProfile(@CurrentUser UserPrincipal userPrincipal,
                                                      @Valid @RequestBody UpdateProfileRequest updateProfileRequest,
                                                      HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(customerService.updateProfile(userPrincipal.getUserId(),
                        updateProfileRequest),
                FeedbackMessage.PROFILE_UPDATED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

}
