package com.syntra.web;

import com.syntra.dto.UserSummary;
import com.syntra.security.CurrentUser;
import com.syntra.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DirectoryController {
    private final AuthService auth;
    private final com.syntra.service.PresenceService presence;

    public DirectoryController(AuthService auth, com.syntra.service.PresenceService presence) {
        this.auth = auth;
        this.presence = presence;
    }

    @GetMapping("/users")
    public List<UserSummary> users() {
        CurrentUser.get();
        return auth.directory();
    }

    @GetMapping("/presence")
    public Map<String, Object> presence() {
        return Map.of("online", presence.onlineUsers());
    }
}
