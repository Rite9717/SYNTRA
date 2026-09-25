package com.syntra.web;

import com.syntra.dto.AdminStats;
import com.syntra.dto.AdminUserResponse;
import com.syntra.dto.CreateUserRequest;
import com.syntra.dto.HealthComponent;
import com.syntra.dto.IncidentResponse;
import com.syntra.dto.SnapshotResponse;
import com.syntra.service.AdminService;
import com.syntra.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final AuthService auth;

    public AdminController(AdminService admin, AuthService auth) {
        this.admin = admin;
        this.auth = auth;
    }

    @GetMapping("/dashboard/stats")
    public AdminStats stats() {
        return admin.stats();
    }

    @GetMapping("/health")
    public List<HealthComponent> health() {
        return admin.health();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) String keyword) {
        return admin.users(keyword);
    }

    @PostMapping("/users")
    public AdminUserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return auth.create(request);
    }

    @PutMapping("/users/{id}/toggle-status")
    public AdminUserResponse toggle(@PathVariable Long id) {
        return admin.toggle(id);
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> incidents() {
        return admin.incidents();
    }

    @PutMapping("/incidents/{id}/acknowledge")
    public IncidentResponse acknowledge(@PathVariable Long id) {
        return admin.acknowledge(id);
    }

    @PutMapping("/incidents/{id}/resolve")
    public IncidentResponse resolve(@PathVariable Long id) {
        return admin.resolve(id);
    }

    @GetMapping("/metrics/recent")
    public List<SnapshotResponse> metrics() {
        return admin.recentMetrics();
    }
}
