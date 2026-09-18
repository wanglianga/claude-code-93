package com.ccb.queue.security;

import com.ccb.queue.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 单实例演示用内存令牌存储 */
@Component
public class TokenStore {
    private final Map<String, User> tokens = new ConcurrentHashMap<>();

    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, user);
        return token;
    }

    public Optional<User> resolve(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(tokens.get(token));
    }
}
