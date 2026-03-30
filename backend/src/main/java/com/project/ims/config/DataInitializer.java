package com.project.ims.config;

import com.project.ims.model.User;
import com.project.ims.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin@123"));
                admin.setEmail("admin@ims.com");
                admin.setFirstName("System");
                admin.setLastName("Administrator");
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now());
                
                Set<String> roles = new HashSet<>();
                roles.add("ROLE_ADMIN");
                roles.add("ROLE_USER");
                admin.setRoles(roles);
                
                userRepository.save(admin);
                System.out.println("Default admin user created successfully!");
                System.out.println("Username: admin");
                System.out.println("Password: admin@123");
            }
        };
    }
}
