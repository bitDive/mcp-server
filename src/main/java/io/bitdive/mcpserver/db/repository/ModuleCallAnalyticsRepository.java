package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.ModuleCallAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ModuleCallAnalyticsRepository extends JpaRepository<ModuleCallAnalytics, Integer> {
    List<ModuleCallAnalytics> findByAnalysisTimestampBetween(OffsetDateTime start, OffsetDateTime end);
}
