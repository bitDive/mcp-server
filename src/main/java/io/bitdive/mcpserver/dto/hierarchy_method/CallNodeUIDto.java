package io.bitdive.mcpserver.dto.hierarchy_method;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CallNodeUIDto {
    private String methodName;
    private String className;
    private Double callTime;
    private List<CallNodeUIDto> children;
}
