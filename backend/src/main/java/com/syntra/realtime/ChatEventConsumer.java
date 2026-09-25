package com.syntra.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntra.dto.ChatResponse;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatEventConsumer {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messaging;

    public ChatEventConsumer(ObjectMapper objectMapper, SimpMessagingTemplate messaging) {
        this.objectMapper = objectMapper;
        this.messaging = messaging;
    }

    @KafkaListener(topics = "syntra.chat")
    public void onChat(String payload) throws Exception {
        ChatResponse event = objectMapper.readValue(payload, ChatResponse.class);
        messaging.convertAndSend("/topic/rooms/" + event.roomId(), event);
    }
}
