package com.syntra.dto;

public record UserSummary(Long id, String username, String email, String firstName, String lastName, boolean active, boolean online) {}
