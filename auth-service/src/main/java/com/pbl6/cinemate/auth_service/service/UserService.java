package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.entity.User;

import java.util.UUID;

public interface UserService {
    boolean isExistedUser(String email);

    User save(User user);

    User findByToken(String contentToken);

    User findById(UUID id);

    User findByEmail(String email);
}
