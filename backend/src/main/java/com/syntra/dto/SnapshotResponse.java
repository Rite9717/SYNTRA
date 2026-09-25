package com.syntra.dto;

import java.time.LocalDateTime;

public record SnapshotResponse(String name, double value, LocalDateTime capturedAt) {}
