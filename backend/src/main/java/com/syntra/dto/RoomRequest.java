package com.syntra.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RoomRequest(@NotBlank String name, List<Long> memberIds) {}
