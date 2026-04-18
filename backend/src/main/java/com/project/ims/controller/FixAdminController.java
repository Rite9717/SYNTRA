package com.project.ims.controller;

import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/fix")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FixAdminController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/admin-role")
    public ResponseEntity<Map<String, Object>> fixAdminRole() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            Set<String> currentRoles = admin.getRoles();
            response.put("username", admin.getUsername());
            response.put("rolesBefore", new HashSet<>(currentRoles));
            
            // Add ROLE_ADMIN if missing
            if (!currentRoles.contains("ROLE_ADMIN")) {
                currentRoles.add("ROLE_ADMIN");
                admin.setRoles(currentRoles);
                userRepository.save(admin);
                response.put("status", "FIXED");
                response.put("message", "ROLE_ADMIN added successfully");
            } else {
                response.put("status", "OK");
                response.put("message", "ROLE_ADMIN already exists");
            }
            
            response.put("rolesAfter", admin.getRoles());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Object>> checkAdmin() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User admin = userRepository.findByUsername("admin")
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            response.put("username", admin.getUsername());
            response.put("email", admin.getEmail());
            response.put("roles", admin.getRoles());
            response.put("active", admin.getActive());
            response.put("hasRoleAdmin", admin.getRoles().contains("ROLE_ADMIN"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
