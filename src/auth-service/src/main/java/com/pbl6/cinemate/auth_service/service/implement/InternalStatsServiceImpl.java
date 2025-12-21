package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.repository.UserRepository;
import com.pbl6.cinemate.auth_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalStatsServiceImpl implements InternalStatsService {

    private final UserRepository userRepository;

    @Override
    public long getUsersCount() {
        return userRepository.count();
    }
}