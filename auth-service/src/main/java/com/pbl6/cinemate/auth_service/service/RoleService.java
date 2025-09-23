package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.entity.Role;

public interface RoleService {
    Role findByName(String name);
}
