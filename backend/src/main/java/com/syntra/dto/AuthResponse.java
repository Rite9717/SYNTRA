package com.syntra.dto;

import java.util.List;

public record AuthResponse(String token, Long id, String username, String email, String firstName, List<String> roles) {}
