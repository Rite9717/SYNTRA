package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplyRequest(@NotBlank String body) {}
