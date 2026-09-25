package com.syntra.web;

import com.syntra.dto.AuthResponse;
import com.syntra.dto.LoginRequest;
import com.syntra.security.CurrentUser;
import com.syntra.security.UserDetailsImpl;
import com.syntra.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        UserDetailsImpl user = CurrentUser.get();
        return new AuthResponse(null, user.getId(), user.getUsername(), user.getEmail(), null, user.getAuthorities().stream()
                .map(item -> item.getAuthority()).toList());
    }
}
