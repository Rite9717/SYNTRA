package com.syntra.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
public class PresenceService {
    private static final Duration TTL = Duration.ofSeconds(45);
    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void touch(String username) {
        redis.opsForValue().set(ttlKey(username), "1", TTL);
        redis.opsForSet().add("presence:users", username);
    }

    public boolean online(String username) {
        return Boolean.TRUE.equals(redis.hasKey(ttlKey(username)));
    }

    public int onlineCount() {
        return onlineUsers().size();
    }

    public Set<String> onlineUsers() {
        Set<String> members = redis.opsForSet().members("presence:users");
        Set<String> live = new HashSet<>();
        if (members == null) {
            return live;
        }
        for (String username : members) {
            if (online(username)) {
                live.add(username);
            } else {
                redis.opsForSet().remove("presence:users", username);
            }
        }
        return live;
    }

    public void incrementUnread(Long userId, Long roomId) {
        redis.opsForHash().increment("unread:" + userId, String.valueOf(roomId), 1);
    }

    public long unread(Long userId, Long roomId) {
        Object value = redis.opsForHash().get("unread:" + userId, String.valueOf(roomId));
        return value == null ? 0 : Long.parseLong(value.toString());
    }

    public void clearUnread(Long userId, Long roomId) {
        redis.opsForHash().delete("unread:" + userId, String.valueOf(roomId));
    }

    private String ttlKey(String username) {
        return "presence:ttl:" + username;
    }
}
