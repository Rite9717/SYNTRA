package com.syntra.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotNull Long userId) {}
