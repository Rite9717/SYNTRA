package com.syntra.service;

import com.syntra.domain.Priority;
import com.syntra.support.PriorityRules;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiClient {
    private final RestClient client;

    public AiClient(@Value("${syntra.ai-url}") String aiUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(4000);
        this.client = RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();
    }

    public Priority priority(String subject, String body) {
        try {
            Map<?, ?> response = client.post().uri("/priority")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("subject", subject, "body", body))
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("priority") != null) {
                return Priority.valueOf(response.get("priority").toString());
            }
        } catch (Exception ignored) {
            // Local rules keep mail moving when the agent is down.
        }
        return PriorityRules.classify(subject, body);
    }

    public String summarize(List<String> texts) {
        try {
            Map<?, ?> response = client.post().uri("/summarize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", texts))
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("summary") != null) {
                return response.get("summary").toString();
            }
        } catch (Exception ignored) {
        }
        return localSummary(texts);
    }

    public String draft(String subject, String instruction, String context) {
        try {
            Map<?, ?> response = client.post().uri("/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "subject", subject == null ? "" : subject,
                            "instruction", instruction == null ? "" : instruction,
                            "context", context == null ? "" : context))
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("draft") != null) {
                return response.get("draft").toString();
            }
        } catch (Exception ignored) {
        }
        return "Regarding " + (subject == null ? "your note" : subject) + ":\n\n" + (instruction == null ? "" : instruction);
    }

    public String ask(String question, List<String> transcripts) {
        try {
            Map<?, ?> response = client.post().uri("/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("question", question, "transcripts", transcripts))
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("answer") != null) {
                return response.get("answer").toString();
            }
        } catch (Exception ignored) {
        }
        return localSummary(transcripts);
    }

    public boolean healthy() {
        try {
            Map<?, ?> response = client.get().uri("/health").retrieve().body(Map.class);
            return response != null && "ok".equals(response.get("status"));
        } catch (Exception ex) {
            return false;
        }
    }

    private String localSummary(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return "Nothing to summarize yet.";
        }
        String joined = String.join(" ", texts);
        return joined.length() > 420 ? joined.substring(0, 420) + "..." : joined;
    }
}
