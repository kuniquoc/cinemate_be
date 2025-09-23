package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.UserPrincipal;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return UserPrincipal.createUserPrincipal(userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND)));
    }
}
