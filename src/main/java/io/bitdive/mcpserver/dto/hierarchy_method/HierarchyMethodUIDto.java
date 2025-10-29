package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public class HierarchyMethodUIDto  {
    private UUID messageId;
    private String className;
    private String methodName;
    private UUID spanId;
    private UUID traceId;
    private String args;
    private OffsetDateTime callTimeDateStart;
    private OffsetDateTime callTimeDateEnd;
    private Double callTimeDelta;
    private String methodReturn;
    private String errorCallMessage;
    private String moduleName;
    private String serviceName;
    private Boolean inPointFlag;
    private Integer codeResponse;
    private String url;
    private String operationType;
    private List<SQLCall> sqlCalls = new ArrayList<>();
    private List<RestCall> restCalls = new ArrayList<>();
    private List<QueueCall> queueCalls = new ArrayList<>();
    private List<HierarchyMethodUIDto> childCalls = new ArrayList<>();
    private String libraryVersion;
    private RestCall restCallMethod;

}
