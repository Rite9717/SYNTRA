package com.syntra.dto;

import jakarta.validation.constraints.NotNull;

public record ForwardRequest(@NotNull Long receiverId, String note) {}
