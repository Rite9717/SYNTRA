package com.project.ims.dto;

import com.project.ims.model.ChatMessage.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private String content;
    private String senderUsername;
    private LocalDateTime timestamp;
    private MessageType type;
}
