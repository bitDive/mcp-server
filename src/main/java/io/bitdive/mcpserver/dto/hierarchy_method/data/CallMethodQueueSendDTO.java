package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CallMethodQueueSendDTO  {
    private UUID messageId;
    private UUID spanId;
    private UUID traceId;
    private UUID callIdMessage;
    private String messageBody;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Long callTimeDelta;
    private String queueServer;
    private String topicName;
    private String libraryVersion;
    private String errorCall;
    private Boolean isError;
}
