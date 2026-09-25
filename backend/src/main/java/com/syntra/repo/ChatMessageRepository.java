package com.syntra.repo;

import com.syntra.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByRoomIdOrderByTimestampDesc(Long roomId);
    long countByTimestampAfter(LocalDateTime after);

    List<ChatMessage> findTop200ByRoomIdAndContentContainingIgnoreCaseOrderByTimestampDesc(Long roomId, String content);
}
