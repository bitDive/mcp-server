package io.bitdive.mcpserver.dto.hierarchy_method.heap_map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AnalyticsBaseDTO {
    private OffsetDateTime timestamp;

    // Metrics
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
    private Integer count2xx;
    private Integer count3xx;
    private Integer count4xx;
    private Integer count5xx;
    private Integer queueSendCount;
    private Double avgQueueSendTime;
    private Integer queueSendErrorCount;
    private Integer queueConsumerCount;
    private Double queueConsumerAvgCallTime;
    private Integer queueConsumerErrorCount;

    private Integer  totalErrorCount;

    public AnalyticsBaseDTO(OffsetDateTime timestamp, Integer errorCount, Integer callCountWeb, Double avgCallTimeWeb,
                            Integer callCountScheduler, Double avgCallTimeScheduler, Integer sqlCallCount, Double avgSqlCallTime,
                            Integer sqlErrorCount, Integer restCallCount, Double avgRestCallTime, Integer restErrorCount,
                            Integer count2xx, Integer count3xx, Integer count4xx, Integer count5xx,Integer queueSendCount ,
                            Double avgQueueSendTime ,Integer queueSendErrorCount ,Integer queueConsumerCount ,Double queueConsumerAvgCallTime ,
                            Integer queueConsumerErrorCount) {
        this.timestamp = timestamp;
        this.errorCount = errorCount;
        this.callCountWeb = callCountWeb;
        this.avgCallTimeWeb = avgCallTimeWeb;
        this.callCountScheduler = callCountScheduler;
        this.avgCallTimeScheduler = avgCallTimeScheduler;
        this.sqlCallCount = sqlCallCount;
        this.avgSqlCallTime = avgSqlCallTime;
        this.sqlErrorCount = sqlErrorCount;
        this.restCallCount = restCallCount;
        this.avgRestCallTime = avgRestCallTime;
        this.restErrorCount = restErrorCount;
        this.count2xx = count2xx;
        this.count3xx = count3xx;
        this.count4xx = count4xx;
        this.count5xx = count5xx;
        this.queueSendCount=queueSendCount;
        this.avgQueueSendTime=avgQueueSendTime;
        this.queueSendErrorCount=queueSendErrorCount;
        this.queueConsumerCount=queueConsumerCount;
        this.queueConsumerAvgCallTime=queueConsumerAvgCallTime;
        this.queueConsumerErrorCount=queueConsumerErrorCount;
        this.totalErrorCount = errorCount+ restErrorCount +sqlErrorCount+queueSendErrorCount+queueConsumerErrorCount;
    }
}