package com.syntra.repo;

import com.syntra.domain.Incident;
import com.syntra.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findTop30ByOrderByCreatedAtDesc();
    long countByStatus(IncidentStatus status);
    Optional<Incident> findFirstByMetricNameAndStatus(String metricName, IncidentStatus status);
}
