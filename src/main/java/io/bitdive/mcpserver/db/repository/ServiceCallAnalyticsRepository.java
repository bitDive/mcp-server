package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.ServiceCallAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ServiceCallAnalyticsRepository extends JpaRepository<ServiceCallAnalytics, Integer> {
    List<ServiceCallAnalytics> findByAnalysisTimestampBetween(OffsetDateTime start, OffsetDateTime end);
}

