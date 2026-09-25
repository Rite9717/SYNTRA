package com.syntra.support;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageRulesTest {
    @Test
    void rejectsEmptyChat() {
        assertThrows(ResponseStatusException.class, () -> MessageRules.cleanChat("   "));
    }

    @Test
    void rejectsLongChat() {
        String content = "a".repeat(2001);
        assertThrows(ResponseStatusException.class, () -> MessageRules.cleanChat(content));
    }

    @Test
    void trimsChat() {
        assertEquals("hello", MessageRules.cleanChat("  hello  "));
    }
}
