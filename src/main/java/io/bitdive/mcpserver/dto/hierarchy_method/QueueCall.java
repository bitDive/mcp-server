package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class QueueCall {
    private UUID messageId;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Double callTimeDelta;
    private String messageBody;
    private String queueTopic;
    private String queueServer;
    private String libraryVersion;
    private String errorCall;
    private Boolean isError;
}
