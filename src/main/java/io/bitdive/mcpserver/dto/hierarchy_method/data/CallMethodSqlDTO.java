package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class CallMethodSqlDTO {
    private Integer id;
    private UUID spanId;
    private UUID traceId;
    private UUID messageId;
    private UUID callIdMessage;
    private String callMethodSqlText;
    private String callMethodSqlParam;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Double callTimeDelta;
    private String errorCallMessage;
    private String libraryVersion;
    private Integer urlConnectId;
}
