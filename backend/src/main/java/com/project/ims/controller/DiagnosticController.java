package com.project.ims.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import com.project.ims.security.UserDetailsImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diagnostic")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DiagnosticController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my-info")
    public ResponseEntity<Map<String, Object>> getMyInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> info = new HashMap<>();
        
        // Current authentication info
        info.put("username", auth.getName());
        info.put("authenticated", auth.isAuthenticated());
        info.put("authoritiesFromToken", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        // Database info
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            User user = userRepository.findById(userDetails.getId()).orElse(null);
            if (user != null) {
                info.put("rolesInDatabase", user.getRoles());
                info.put("userId", user.getId());
                info.put("email", user.getEmail());
                info.put("active", user.getActive());
            }
        } catch (Exception e) {
            info.put("databaseError", e.getMessage());
        }
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/check-admin")
    public ResponseEntity<Map<String, Object>> checkAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new HashMap<>();
        
        boolean hasRoleAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        result.put("hasRoleAdmin", hasRoleAdmin);
        result.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        if (!hasRoleAdmin) {
            result.put("message", "You don't have ROLE_ADMIN in your current token");
            result.put("solution", "Logout and login again to get a fresh token with updated roles");
        } else {
            result.put("message", "You have ROLE_ADMIN - access should work");
        }
        
        return ResponseEntity.ok(result);
    }
}
