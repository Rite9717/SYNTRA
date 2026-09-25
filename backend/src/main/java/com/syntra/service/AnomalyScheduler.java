package com.syntra.service;

import com.syntra.domain.Incident;
import com.syntra.domain.IncidentStatus;
import com.syntra.domain.MetricSnapshot;
import com.syntra.repo.ChatMessageRepository;
import com.syntra.repo.IncidentRepository;
import com.syntra.repo.MailRepository;
import com.syntra.repo.MetricSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AnomalyScheduler {
    private final MailRepository mail;
    private final ChatMessageRepository chat;
    private final IncidentRepository incidents;
    private final MetricSnapshotRepository snapshots;
    private final SyntraMetrics metrics;
    private final PresenceService presence;
    private final AtomicLong previousRate = new AtomicLong(-1);
    private final AtomicLong previousAuthFailures = new AtomicLong(0);

    public AnomalyScheduler(MailRepository mail, ChatMessageRepository chat, IncidentRepository incidents,
                            MetricSnapshotRepository snapshots, SyntraMetrics metrics, PresenceService presence) {
        this.mail = mail;
        this.chat = chat;
        this.incidents = incidents;
        this.snapshots = snapshots;
        this.metrics = metrics;
        this.presence = presence;
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    @Transactional
    public void scan() {
        LocalDateTime minuteAgo = LocalDateTime.now().minusMinutes(1);
        long rate = mail.countBySentAtAfter(minuteAgo) + chat.countByTimestampAfter(minuteAgo);
        long previous = previousRate.getAndSet(rate);
        if (previous >= 15 && rate < previous * 0.25) {
            open("Message rate dropped",
                    "Delivery volume fell from " + previous + " to " + rate + " in one minute. Check Kafka and the realtime broker.",
                    "WARNING", "message_rate", rate);
        }
        long failures = Math.round(metrics.authFailureCount());
        long failureDelta = failures - previousAuthFailures.getAndSet(failures);
        if (failureDelta >= 8) {
            open("Authentication failures spiked",
                    failureDelta + " failed logins in the last minute.",
                    "CRITICAL", "auth_failures", failureDelta);
        }
        capture("message_rate", rate);
        capture("auth_failures_total", failures);
        capture("online_users", presence.onlineCount());
        snapshots.deleteByCapturedAtBefore(LocalDateTime.now().minusHours(24));
    }

    private void open(String title, String detail, String severity, String metric, double value) {
        if (incidents.findFirstByMetricNameAndStatus(metric, IncidentStatus.OPEN).isPresent()) {
            return;
        }
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDetail(detail);
        incident.setSeverity(severity);
        incident.setMetricName(metric);
        incident.setMetricValue(value);
        incidents.save(incident);
    }

    private void capture(String name, double value) {
        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setName(name);
        snapshot.setValue(value);
        snapshots.save(snapshot);
    }
}
