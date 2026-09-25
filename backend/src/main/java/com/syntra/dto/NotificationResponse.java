package com.syntra.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String type, String title, String body, boolean read, LocalDateTime createdAt, String link) {}
