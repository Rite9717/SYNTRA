package com.syntra.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "metric_snapshots")
@Getter
@Setter
public class MetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private LocalDateTime capturedAt = LocalDateTime.now();
}
