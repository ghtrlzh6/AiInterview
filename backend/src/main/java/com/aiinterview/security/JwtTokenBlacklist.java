package com.aiinterview.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenBlacklist {

    private final ConcurrentHashMap<String, Long> entries = new ConcurrentHashMap<>();

    public void add(String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        entries.put(token, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    public boolean contains(String token) {
        Long expireAt = entries.get(token);
        if (expireAt == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expireAt) {
            entries.remove(token);
            return false;
        }
        return true;
    }
}
