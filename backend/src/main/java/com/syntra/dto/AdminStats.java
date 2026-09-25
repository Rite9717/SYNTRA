package com.syntra.dto;

import java.util.List;

public record AdminStats(long totalUsers, long activeUsers, long totalMail, long mailToday, long unreadMail,
                         long chatMessages, long openIncidents, int onlineUsers, String grafanaUrl,
                         List<HealthComponent> health) {}
