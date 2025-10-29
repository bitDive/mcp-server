package io.bitdive.mcpserver.db.repository;

import io.bitdive.mcpserver.db.entity.InPointCallAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface InPointCallAnalyticsRepository extends JpaRepository<InPointCallAnalytics, Integer> {
    List<InPointCallAnalytics> findByAnalysisTimestampBetween(OffsetDateTime start, OffsetDateTime end);
}
