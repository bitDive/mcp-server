package io.bitdive.mcpserver.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "module_call_analytics")
@Getter
public class ModuleCallAnalytics {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;
    private Integer idReport;
    private String moduleName;
    private Integer errorCount;
    private Integer callCountWeb;
    private Double avgCallTimeWeb;
    private Integer callCountScheduler;
    private Double avgCallTimeScheduler;
    private Integer sqlCallCount;
    private Double avgSqlCallTime;
    private Integer sqlErrorCount;
    private Integer restCallCount;
    private Double avgRestCallTime;
    private Integer restErrorCount;
    @Column(name = "count_2xx")
    private Integer count2xx;
    @Column(name = "count_3xx")
    private Integer count3xx;
    @Column(name = "count_4xx")
    private Integer count4xx;
    @Column(name = "count_5xx")
    private Integer count5xx;

    private Integer queueSendCount;
    private Double avgQueueSendTime;
    private Integer queueSendErrorCount;
    private Integer queueConsumerCount;
    private Double queueConsumerAvgCallTime;
    private Integer queueConsumerErrorCount;


    private OffsetDateTime analysisTimestamp;
}
