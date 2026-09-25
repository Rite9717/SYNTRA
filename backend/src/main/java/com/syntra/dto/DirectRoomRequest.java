package com.syntra.dto;

import jakarta.validation.constraints.NotNull;

public record DirectRoomRequest(@NotNull Long userId) {}
