package com.project.ims.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    private Boolean active;
    private Set<String> roles;
}
