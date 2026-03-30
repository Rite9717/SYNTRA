package com.project.ims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ims.config.KafkaConfig;
import com.project.ims.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.CHAT_TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consumeChatMessage(String messageJson) {
        try {
            ChatMessageDTO message = objectMapper.readValue(messageJson, ChatMessageDTO.class);
            messagingTemplate.convertAndSend("/topic/public", message);
        } catch (Exception e) {
            System.err.println("Error processing Kafka message: " + e.getMessage());
        }
    }
}
