package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class SQLCall {
    private UUID messageId;
    private String sql;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Double callTimeDelta;
    private String errorCallMessage;
    private String libraryVersion;

}
