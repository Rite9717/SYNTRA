package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MailRequest(@NotNull Long receiverId, @NotBlank String subject, @NotBlank String body) {}
