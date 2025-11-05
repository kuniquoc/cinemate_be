package com.pbl6.cinemate.auth_service.repository;

import com.pbl6.cinemate.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    List<User> findByRoleId(UUID roleId);

    boolean existsByEmail(String email);

    List<User> findAllByIsEnabled(Boolean isEnabled);
}
