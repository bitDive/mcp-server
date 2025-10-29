package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class InPointUIDto {
    @EqualsAndHashCode.Exclude
    private Long inPointId;
    private String inPointName;
    private Boolean IsInPoint;
}
