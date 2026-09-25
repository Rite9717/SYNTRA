package com.syntra.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(Long id, String username, String email, String firstName, String lastName,
                                boolean active, List<String> roles, LocalDateTime createdAt, LocalDateTime lastLogin) {}
