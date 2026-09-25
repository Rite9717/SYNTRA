package com.syntra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntra.dto.ChatResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messaging;

    public EventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper, SimpMessagingTemplate messaging) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.messaging = messaging;
    }

    public void chat(ChatResponse event) {
        try {
            kafka.send("syntra.chat", String.valueOf(event.roomId()), objectMapper.writeValueAsString(event))
                    .get(2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            messaging.convertAndSend("/topic/rooms/" + event.roomId(), event);
        }
    }

    public void mail(Long receiverId, String subject) {
        try {
            kafka.send("syntra.mail", String.valueOf(receiverId),
                    objectMapper.writeValueAsString(Map.of("receiverId", receiverId, "subject", subject)));
        } catch (JsonProcessingException | RuntimeException ignored) {
            // Mail is already stored. Live delivery is best-effort.
        }
    }

    public void broadcast(String destination, Object payload) {
        messaging.convertAndSend(destination, payload);
    }

    public void toUser(String username, Object payload) {
        messaging.convertAndSendToUser(username, "/queue/notifications", payload);
    }
}
