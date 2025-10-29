package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class CallMethodRestDTO {
    private UUID traceId;
    private UUID spanId;
    private UUID messageId;
    private UUID callIdMessage;
    private OffsetDateTime dateStart;
    private OffsetDateTime dateEnd;
    private Double callTimeDelta;
    private String uri;
    private String methodId;
    private String headers;
    private String body;
    private String statusCode;
    private String responseHeaders;
    private String responseBody;
    private String errorCall;
    private String libraryVersion;
}
