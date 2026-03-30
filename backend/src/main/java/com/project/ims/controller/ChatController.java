package com.project.ims.controller;

import com.project.ims.dto.ChatMessageDTO;
import com.project.ims.model.ChatMessage;
import com.project.ims.model.ChatMessage.MessageType;
import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import com.project.ims.service.ChatService;
import com.project.ims.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO messageDTO, Principal principal) {
        // Validate message content
        if (messageDTO.getContent() == null || messageDTO.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        if (messageDTO.getContent().length() > 2000) {
            throw new IllegalArgumentException("Message content cannot exceed 2000 characters");
        }

        // Get authenticated user
        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save message to database
        ChatMessage savedMessage = chatService.saveMessage(
                messageDTO.getContent().trim(), 
                sender, 
                MessageType.CHAT
        );

        // Create response DTO
        ChatMessageDTO response = new ChatMessageDTO();
        response.setId(savedMessage.getId());
        response.setContent(savedMessage.getContent());
        response.setSenderUsername(sender.getUsername());
        response.setTimestamp(savedMessage.getTimestamp());
        response.setType(MessageType.CHAT);

        // Send to Kafka
        kafkaProducerService.sendChatMessage(response);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageDTO messageDTO, Principal principal) {
        // Get authenticated user
        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create join notification
        ChatMessageDTO response = new ChatMessageDTO();
        response.setContent(username + " joined the chat");
        response.setSenderUsername(username);
        response.setTimestamp(LocalDateTime.now());
        response.setType(MessageType.JOIN);

        // Send to Kafka
        kafkaProducerService.sendChatMessage(response);
    }
}
