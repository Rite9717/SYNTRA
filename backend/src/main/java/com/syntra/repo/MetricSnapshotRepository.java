package com.syntra.repo;

import com.syntra.domain.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {
    List<MetricSnapshot> findTop180ByOrderByCapturedAtDesc();
    void deleteByCapturedAtBefore(LocalDateTime cutoff);
}
