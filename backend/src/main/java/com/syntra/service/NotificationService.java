package com.syntra.service;

import com.syntra.domain.AppNotification;
import com.syntra.domain.User;
import com.syntra.dto.NotificationResponse;
import com.syntra.repo.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final EventPublisher events;

    public NotificationService(NotificationRepository notifications, EventPublisher events) {
        this.notifications = notifications;
        this.events = events;
    }

    @Transactional
    public void push(User user, String type, String title, String body, String link) {
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(trim(body));
        notification.setLink(link);
        AppNotification saved = notifications.save(notification);
        events.toUser(user.getUsername(), toResponse(saved));
    }

    public List<NotificationResponse> list(Long userId) {
        return notifications.findTop40ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    public long unread(Long userId) {
        return notifications.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        AppNotification notification = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.setReadFlag(true);
    }

    @Transactional
    public void markAll(Long userId) {
        notifications.findByUserIdAndReadFlagFalse(userId).forEach(item -> item.setReadFlag(true));
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.isReadFlag(), notification.getCreatedAt(), notification.getLink());
    }

    private String trim(String body) {
        if (body == null) return "";
        return body.length() > 480 ? body.substring(0, 480) : body;
    }
}
