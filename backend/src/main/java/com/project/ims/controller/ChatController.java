package com.project.ims.controller;

import com.project.ims.dto.ChatHistoryResponse;
import com.project.ims.dto.ChatMessageDTO;
import com.project.ims.model.ChatMessage;
import com.project.ims.model.ChatMessage.MessageType;
import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import com.project.ims.service.ChatService;
import com.project.ims.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
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
        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatMessage savedMessage = chatService.saveMessage(
                messageDTO.getContent().trim(), 
                sender, 
                MessageType.CHAT
        );
        ChatMessageDTO response = new ChatMessageDTO();
        response.setId(savedMessage.getId());
        response.setContent(savedMessage.getContent());
        response.setSenderUsername(sender.getUsername());
        response.setTimestamp(savedMessage.getTimestamp());
        response.setType(MessageType.CHAT);
        kafkaProducerService.sendChatMessage(response);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageDTO messageDTO, Principal principal) {
        // Get authenticated user
        String username = principal.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatMessageDTO response = new ChatMessageDTO();
        response.setContent(username + " joined the chat");
        response.setSenderUsername(username);
        response.setTimestamp(LocalDateTime.now());
        response.setType(MessageType.JOIN);
        kafkaProducerService.sendChatMessage(response);
    }
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory() {
        List<ChatMessageDTO> messages = chatService.getRecentMessages(50);
        ChatHistoryResponse response = new ChatHistoryResponse(messages, messages.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendMessageRest(
            @RequestBody ChatMessageDTO messageDTO, 
            Authentication authentication) {
        if (messageDTO.getContent() == null || messageDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (messageDTO.getContent().length() > 2000) {
            return ResponseEntity.badRequest().build();
        }
        String username = authentication.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save message to database
        ChatMessage savedMessage = chatService.saveMessage(
                messageDTO.getContent().trim(), 
                sender, 
                MessageType.CHAT
        );
        ChatMessageDTO response = new ChatMessageDTO();
        response.setId(savedMessage.getId());
        response.setContent(savedMessage.getContent());
        response.setSenderUsername(sender.getUsername());
        response.setTimestamp(savedMessage.getTimestamp());
        response.setType(MessageType.CHAT);
        kafkaProducerService.sendChatMessage(response);

        return ResponseEntity.ok(response);
    }
    @PostMapping("/join")
    public ResponseEntity<ChatMessageDTO> joinChatRest(Authentication authentication)
    {
        String username = authentication.getName();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatMessageDTO response = new ChatMessageDTO();
        response.setContent(username + " joined the chat");
        response.setSenderUsername(username);
        response.setTimestamp(LocalDateTime.now());
        response.setType(MessageType.JOIN);
        kafkaProducerService.sendChatMessage(response);

        return ResponseEntity.ok(response);
    }
}
