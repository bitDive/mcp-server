package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RestCall {
    private UUID messageId;
    private String uri;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Double callTimeDelta;
    private String method;
    private String headers;
    private String body;
    private String statusCode;
    public String responseHeaders;
    public String responseBody;
    private String errorCallMessage;
    private String libraryVersion;
}
