package com.syntra.service;

import com.syntra.domain.User;
import com.syntra.dto.AdminUserResponse;
import com.syntra.dto.AuthResponse;
import com.syntra.dto.CreateUserRequest;
import com.syntra.dto.LoginRequest;
import com.syntra.dto.UserSummary;
import com.syntra.repo.UserRepository;
import com.syntra.security.JwtUtils;
import com.syntra.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SyntraMetrics metrics;
    private final PresenceService presence;

    public AuthService(AuthenticationManager authenticationManager, UserRepository users, PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils, SyntraMetrics metrics, PresenceService presence) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.metrics = metrics;
        this.presence = presence;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
            User user = users.findById(principal.getId()).orElseThrow();
            user.setLastLogin(LocalDateTime.now());
            presence.touch(user.getUsername());
            return new AuthResponse(jwtUtils.generate(user.getUsername(), user.getId()), user.getId(), user.getUsername(),
                    user.getEmail(), user.getFirstName(), List.copyOf(user.getRoles()));
        } catch (Exception ex) {
            metrics.authFailure();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    @Transactional
    public AdminUserResponse create(CreateUserRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (users.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        String role = request.role().toUpperCase(Locale.ROOT);
        if (!role.equals("USER") && !role.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRoles(Set.of("ROLE_" + role));
        return toAdmin(users.save(user));
    }

    public List<UserSummary> directory() {
        return users.findByActiveTrueOrderByUsernameAsc().stream().map(this::toSummary).toList();
    }

    public User require(Long id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User requireUsername(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.isActive(), presence.online(user.getUsername()));
    }

    public AdminUserResponse toAdmin(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.isActive(), List.copyOf(user.getRoles()), user.getCreatedAt(), user.getLastLogin());
    }
}
