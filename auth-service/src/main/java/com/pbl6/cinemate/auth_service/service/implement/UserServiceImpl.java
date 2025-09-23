package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.auth_service.service.TokenService;
import com.pbl6.cinemate.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Override
    public boolean isExistedUser(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByToken(String contentToken) {
        return tokenService.findTokenByContent(contentToken).getUser();
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
    }
}
