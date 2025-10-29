package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.ClassCallAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ClassCallAnalyticsRepository extends JpaRepository<ClassCallAnalytics, Integer> {
    List<ClassCallAnalytics> findByAnalysisTimestampBetween(OffsetDateTime start, OffsetDateTime end);
}
