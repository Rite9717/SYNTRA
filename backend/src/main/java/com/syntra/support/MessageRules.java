package com.syntra.support;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class MessageRules {
    private MessageRules() {}

    public static String cleanChat(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot be empty");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content cannot exceed 2000 characters");
        }
        return trimmed;
    }

    public static String requireText(String value, String label, int max) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " cannot be empty");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " cannot exceed " + max + " characters");
        }
        return trimmed;
    }
}
