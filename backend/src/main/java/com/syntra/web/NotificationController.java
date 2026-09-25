package com.syntra.web;

import com.syntra.dto.NotificationResponse;
import com.syntra.security.CurrentUser;
import com.syntra.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notifications.list(CurrentUser.get().getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("count", notifications.unread(CurrentUser.get().getId()));
    }

    @PutMapping("/{id}/read")
    public Map<String, String> read(@PathVariable Long id) {
        notifications.markRead(CurrentUser.get().getId(), id);
        return Map.of("message", "Read");
    }

    @PutMapping("/read-all")
    public Map<String, String> readAll() {
        notifications.markAll(CurrentUser.get().getId());
        return Map.of("message", "Read");
    }
}
