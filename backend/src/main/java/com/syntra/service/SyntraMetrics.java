package com.syntra.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SyntraMetrics {
    private final Counter mailSent;
    private final Counter chatSent;
    private final Counter authFailures;

    public SyntraMetrics(MeterRegistry registry) {
        this.mailSent = Counter.builder("syntra.mail.sent").description("Private messages sent").register(registry);
        this.chatSent = Counter.builder("syntra.chat.sent").description("Chat messages sent").register(registry);
        this.authFailures = Counter.builder("syntra.auth.failures").description("Failed logins").register(registry);
    }

    public void mailSent() { mailSent.increment(); }
    public void chatSent() { chatSent.increment(); }
    public void authFailure() { authFailures.increment(); }
    public double authFailureCount() { return authFailures.count(); }
}
