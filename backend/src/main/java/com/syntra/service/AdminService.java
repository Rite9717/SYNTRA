package com.syntra.service;

import com.syntra.domain.Incident;
import com.syntra.domain.IncidentStatus;
import com.syntra.domain.MetricSnapshot;
import com.syntra.domain.User;
import com.syntra.dto.AdminStats;
import com.syntra.dto.AdminUserResponse;
import com.syntra.dto.HealthComponent;
import com.syntra.dto.IncidentResponse;
import com.syntra.dto.SnapshotResponse;
import com.syntra.repo.ChatMessageRepository;
import com.syntra.repo.IncidentRepository;
import com.syntra.repo.MailRepository;
import com.syntra.repo.MetricSnapshotRepository;
import com.syntra.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {
    private final UserRepository users;
    private final MailRepository mail;
    private final ChatMessageRepository chat;
    private final IncidentRepository incidents;
    private final MetricSnapshotRepository snapshots;
    private final AuthService auth;
    private final PresenceService presence;
    private final AiClient ai;
    private final DataSource dataSource;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafka;
    private final String grafanaUrl;

    public AdminService(UserRepository users, MailRepository mail, ChatMessageRepository chat, IncidentRepository incidents,
                        MetricSnapshotRepository snapshots, AuthService auth, PresenceService presence, AiClient ai,
                        DataSource dataSource, StringRedisTemplate redis, KafkaTemplate<String, String> kafka,
                        @Value("${syntra.grafana-url}") String grafanaUrl) {
        this.users = users;
        this.mail = mail;
        this.chat = chat;
        this.incidents = incidents;
        this.snapshots = snapshots;
        this.auth = auth;
        this.presence = presence;
        this.ai = ai;
        this.dataSource = dataSource;
        this.redis = redis;
        this.kafka = kafka;
        this.grafanaUrl = grafanaUrl;
    }

    public AdminStats stats() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return new AdminStats(
                users.count(),
                users.countByActiveTrue(),
                mail.count(),
                mail.countBySentAtAfter(start),
                mail.countByReadFlagFalseAndDeletedFalse(),
                chat.count(),
                incidents.countByStatus(IncidentStatus.OPEN),
                presence.onlineCount(),
                grafanaUrl,
                health());
    }

    public List<AdminUserResponse> users(String keyword) {
        List<User> found = (keyword == null || keyword.isBlank())
                ? users.findAll()
                : users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                keyword, keyword, keyword, keyword);
        return found.stream().map(auth::toAdmin).toList();
    }

    @Transactional
    public AdminUserResponse toggle(Long id) {
        User user = auth.require(id);
        user.setActive(!user.isActive());
        return auth.toAdmin(user);
    }

    public List<IncidentResponse> incidents() {
        return incidents.findTop30ByOrderByCreatedAtDesc().stream().map(this::toIncident).toList();
    }

    @Transactional
    public IncidentResponse acknowledge(Long id) {
        Incident incident = load(id);
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        return toIncident(incident);
    }

    @Transactional
    public IncidentResponse resolve(Long id) {
        Incident incident = load(id);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        return toIncident(incident);
    }

    public List<SnapshotResponse> recentMetrics() {
        return snapshots.findTop180ByOrderByCapturedAtDesc().stream()
                .map(item -> new SnapshotResponse(item.getName(), item.getValue(), item.getCapturedAt()))
                .toList();
    }

    public List<HealthComponent> health() {
        List<HealthComponent> components = new ArrayList<>();
        components.add(probe("mysql", this::mysql));
        components.add(probe("redis", this::redisPing));
        components.add(probe("kafka", this::kafkaPing));
        components.add(ai.healthy()
                ? new HealthComponent("ai", "UP", "Agent responded")
                : new HealthComponent("ai", "DOWN", "Agent unreachable"));
        return components;
    }

    private HealthComponent probe(String name, Runnable check) {
        try {
            check.run();
            return new HealthComponent(name, "UP", "Reachable");
        } catch (Exception ex) {
            return new HealthComponent(name, "DOWN", ex.getMessage() == null ? "Unreachable" : ex.getMessage());
        }
    }

    private void mysql() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                throw new IllegalStateException("Connection invalid");
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage() == null ? "MySQL unreachable" : ex.getMessage());
        }
    }

    private void redisPing() {
        String pong = redis.getConnectionFactory().getConnection().ping();
        if (pong == null) {
            throw new IllegalStateException("No ping");
        }
    }

    private void kafkaPing() {
        kafka.metrics();
    }

    private Incident load(Long id) {
        return incidents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    private IncidentResponse toIncident(Incident incident) {
        return new IncidentResponse(incident.getId(), incident.getTitle(), incident.getDetail(), incident.getSeverity(),
                incident.getStatus().name(), incident.getMetricName(), incident.getMetricValue(),
                incident.getCreatedAt(), incident.getResolvedAt());
    }
}
