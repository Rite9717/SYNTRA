package com.syntra.support;

import com.syntra.domain.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityRulesTest {
    @Test
    void marksDeadlinesUrgent() {
        assertEquals(Priority.URGENT, PriorityRules.classify("Lab", "The deadline moved"));
    }

    @Test
    void marksFyi() {
        assertEquals(Priority.FYI, PriorityRules.classify("Notes", "FYI the room changed"));
    }

    @Test
    void defaultsToNormal() {
        assertEquals(Priority.NORMAL, PriorityRules.classify("Lunch", "Are you free at 1?"));
    }
}
