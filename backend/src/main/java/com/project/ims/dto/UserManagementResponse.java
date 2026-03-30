package com.project.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Set<String> roles;
    private Long sentMessagesCount;
    private Long receivedMessagesCount;
}
