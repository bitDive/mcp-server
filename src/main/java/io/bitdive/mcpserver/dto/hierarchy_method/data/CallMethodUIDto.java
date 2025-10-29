package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CallMethodUIDto {
    private UUID id;
    private UUID traceId;
    private String className;
    private String methodName;
    private String args;
    private MessageKafkaCallTimeUIDto messageKafkaCallTime;
    private MessageKafkaParentMethodUIDto parentMethod;
    private String methodReturn;
    private String errorCallMessage;
    private String libraryVersion;
}
