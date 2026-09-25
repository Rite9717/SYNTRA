package com.syntra.support;

import com.syntra.domain.Priority;

public final class PriorityRules {
    private PriorityRules() {}

    public static Priority classify(String subject, String body) {
        String text = ((subject == null ? "" : subject) + " " + (body == null ? "" : body)).toLowerCase();
        if (containsAny(text, "urgent", "asap", "immediately", "critical", "deadline", "eod", "blocker")) {
            return Priority.URGENT;
        }
        if (containsAny(text, "fyi", "newsletter", "optional", "heads up", "for your information")) {
            return Priority.FYI;
        }
        return Priority.NORMAL;
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
