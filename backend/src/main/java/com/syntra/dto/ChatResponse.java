package com.syntra.dto;

import java.time.LocalDateTime;

public record ChatResponse(Long id, Long roomId, String content, String senderUsername, String type, LocalDateTime timestamp) {}
