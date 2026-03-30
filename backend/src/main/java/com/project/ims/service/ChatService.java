package com.project.ims.service;

import com.project.ims.dto.ChatMessageDTO;
import com.project.ims.model.ChatMessage;
import com.project.ims.model.ChatMessage.MessageType;
import com.project.ims.model.User;
import com.project.ims.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage saveMessage(String content, User sender, MessageType type) {
        ChatMessage message = new ChatMessage();
        message.setContent(content);
        message.setSender(sender);
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        
        return chatMessageRepository.save(message);
    }

    public List<ChatMessageDTO> getRecentMessages(int limit) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByOrderByTimestampDesc();
        
        // Reverse to get chronological order
        return messages.stream()
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setTimestamp(message.getTimestamp());
        dto.setType(message.getType());
        return dto;
    }
}
