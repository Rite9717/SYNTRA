package com.syntra.repo;

import com.syntra.domain.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop40ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFlagFalse(Long userId);
    List<AppNotification> findByUserIdAndReadFlagFalse(Long userId);
}
