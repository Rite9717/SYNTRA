package com.project.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStats {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalMessages;
    private Long messagesToday;
    private Long unreadMessages;
}
