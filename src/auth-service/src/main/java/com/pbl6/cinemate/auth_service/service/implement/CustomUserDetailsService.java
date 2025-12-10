package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.shared.constants.ErrorMessage;
import com.pbl6.cinemate.shared.exception.NotFoundException;
import com.pbl6.cinemate.shared.security.UserPrincipal;

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
        return UserPrincipal.createUserPrincipal(user.getId().toString(), user.getEmail(), user.getPassword(),
                user.getRole().getName(),
                user.getRole().getPermissions().stream().map(permission -> permission.getName()).toList(),
                user.getFirstName(), user.getLastName());
    }
}
