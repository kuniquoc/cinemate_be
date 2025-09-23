package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.repository.RoleRepository;
import com.pbl6.cinemate.auth_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
    }
}
