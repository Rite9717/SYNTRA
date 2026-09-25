package com.syntra.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoomSummary(Long id, String name, String type, String lastMessage, LocalDateTime lastMessageAt,
                          long unread, List<MemberSummary> members) {}
