package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CallMethodDTO {
    private UUID id;
    private UUID traceId;
    private UUID spanId;
    private String className;
    private String methodName;
    private UUID callIdParent;
    private String args;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private String methodReturn;
    private String errorCallMessage;
    private Long inPointId;
    private String moduleName;
    private String serviceName;
    private Boolean inPointFlag;
    private Double callTimeDelta;
    private String operationType;
    private Integer codeResponse;
    private List<CallMethodSqlDTO> callMethodSqlList;
    private List<CallMethodRestDTO> callMethodRests;
    private List<CallMethodQueueSendDTO> callMethodQueue;
    private String libraryVersion;
    private CallMethodRestDTO restCallMethod;
}