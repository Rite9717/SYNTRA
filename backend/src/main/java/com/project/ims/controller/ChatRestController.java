package com.project.ims.controller;

import com.project.ims.dto.ChatHistoryResponse;
import com.project.ims.dto.ChatMessageDTO;
import com.project.ims.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory() {
        List<ChatMessageDTO> messages = chatService.getRecentMessages(50);
        ChatHistoryResponse response = new ChatHistoryResponse(messages, messages.size());
        return ResponseEntity.ok(response);
    }
}
