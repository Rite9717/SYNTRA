package com.syntra.dto;

import java.time.LocalDateTime;

public record IncidentResponse(Long id, String title, String detail, String severity, String status,
                               String metricName, double metricValue, LocalDateTime createdAt, LocalDateTime resolvedAt) {}
