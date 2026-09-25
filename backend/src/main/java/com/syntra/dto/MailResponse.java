package com.syntra.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MailResponse(
        Long id, Long senderId, String senderUsername, String senderName,
        Long receiverId, String receiverUsername, String receiverName,
        String subject, String body, LocalDateTime sentAt, LocalDateTime readAt,
        boolean read, boolean starred, boolean archived, String priority,
        Long threadId, Long parentId, Long folderId, String folderName,
        List<AttachmentResponse> attachments
) {}
