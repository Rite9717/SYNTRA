package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;

public record FolderRequest(@NotBlank String name) {}
