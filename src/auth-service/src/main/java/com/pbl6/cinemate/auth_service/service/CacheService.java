package com.pbl6.cinemate.auth_service.service;


import java.util.concurrent.TimeUnit;

public interface CacheService {
    void set(String key, Object value, long timeout, TimeUnit unit);

    void set(String key, Object value);

    Object get(String key);

    void delete(String key);

    boolean hasKey(String key);
}
