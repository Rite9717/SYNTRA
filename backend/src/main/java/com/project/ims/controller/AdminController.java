package com.project.ims.controller;

import com.project.ims.dto.AdminDashboardStats;
import com.project.ims.dto.SignupRequest;
import com.project.ims.dto.UpdateUserRequest;
import com.project.ims.dto.UserManagementResponse;
import com.project.ims.service.AdminService;
import com.project.ims.service.AuthService;
import jakarta.validation.Valid;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    @PostMapping("/user/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@Valid @RequestBody SignupRequest request){
        String message = authService.signup(request);
        return ResponseEntity.ok(message);
    }
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        AdminDashboardStats stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserManagementResponse>> getAllUsers() {
        List<UserManagementResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementResponse> getUserById(@PathVariable Long id) {
        UserManagementResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        UserManagementResponse updatedUser = adminService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id) {
        adminService.toggleUserStatus(id);
        return ResponseEntity.ok("User status toggled successfully");
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok("User deactivated successfully");
    }

    @GetMapping("/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserManagementResponse>> searchUsers(@RequestParam String keyword) {
        List<UserManagementResponse> users = adminService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }
}
