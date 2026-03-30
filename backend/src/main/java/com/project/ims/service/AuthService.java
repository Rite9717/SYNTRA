package com.project.ims.service;

import com.project.ims.dto.AuthResponse;
import com.project.ims.dto.LoginRequest;
import com.project.ims.dto.SignupRequest;
import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import com.project.ims.security.JwtUtils;
import com.project.ims.security.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {
    
	@Autowired
    private AuthenticationManager authenticationManager;
	@Autowired
    private UserRepository userRepository;
	@Autowired
    private PasswordEncoder passwordEncoder;
	@Autowired
    private JwtUtils jwtUtils;
    
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Update last login
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        return new AuthResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                new HashSet<>(userDetails.getAuthorities().stream()
                        .map(item -> item.getAuthority())
                        .toList())
        );
    }
    
    public String signup(SignupRequest signupRequest)
    {
        if(!signupRequest.getEmail().endsWith("@msit.edu.in"))
        {
            throw new RuntimeException("Registration is only allowed with a college email address");
        }
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }
        
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setPhone(signupRequest.getPhone());
        
        Set<String> roles = new HashSet<>();
        roles.add(signupRequest.getRole() != null ? signupRequest.getRole() : "ROLE_USER");
        user.setRoles(roles);
        
        userRepository.save(user);
        
        return "User registered successfully!";
    }
}
