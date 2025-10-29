package io.bitdive.mcpserver.dto.hierarchy_method.heap_map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ClassAnalyticsDTO extends AnalyticsBaseDTO {
    private String className;
    private List<InPointAnalyticsDTO> inPoints;

    public ClassAnalyticsDTO(
            String className,
            OffsetDateTime timestamp,
            Integer errorCount,
            Integer callCountWeb,
            Double avgCallTimeWeb,
            Integer callCountScheduler,
            Double avgCallTimeScheduler,
            Integer sqlCallCount,
            Double avgSqlCallTime,
            Integer sqlErrorCount,
            Integer restCallCount,
            Double avgRestCallTime,
            Integer restErrorCount,
            Integer count2xx,
            Integer count3xx,
            Integer count4xx,
            Integer count5xx,
            Integer queueSendCount ,
            Double avgQueueSendTime ,
            Integer queueSendErrorCount ,
            Integer queueConsumerCount ,
            Double queueConsumerAvgCallTime ,
            Integer queueConsumerErrorCount,
            List<InPointAnalyticsDTO> inPoints
    ) {
        super(timestamp, errorCount, callCountWeb, avgCallTimeWeb, callCountScheduler,
                avgCallTimeScheduler, sqlCallCount, avgSqlCallTime, sqlErrorCount,
                restCallCount, avgRestCallTime, restErrorCount, count2xx, count3xx,
                count4xx, count5xx,queueSendCount ,avgQueueSendTime ,queueSendErrorCount ,
                queueConsumerCount ,queueConsumerAvgCallTime ,queueConsumerErrorCount);
        this.className = className;
        this.inPoints = inPoints;
    }
}
